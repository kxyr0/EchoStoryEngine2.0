from __future__ import annotations

import math
from pathlib import Path

import bpy

from .context import preferences
from .fileio import atomic_write, overwrite_allowed
from .model import ExportAnimationModel, ExportResult, RootMotion
from .naming import resource_id, sanitize_name
from .sampling import sample_tracks, validate_rig
from .serialization import render_animation, render_clip
from .timeline import build_points_events, build_segments, duration_ticks, frame_range, frame_to_tick, required_frames, tick_to_frame
from .validation import loop_seam_issues, root_motion_issues, store_result, validate_structure


def _output(metadata, context) -> Path:
    if metadata.output_directory:
        return Path(bpy.path.abspath(metadata.output_directory))
    addon = preferences(context)
    return Path(bpy.path.abspath(addon.output_root)) if addon else Path.cwd()


def _folder(metadata, context) -> str:
    addon = preferences(context)
    value = metadata.resource_folder or (addon.resource_folder if addon else "")
    return "/".join(sanitize_name(part) for part in value.replace("\\", "/").split("/") if part)


def output_paths(metadata, context) -> tuple[Path, Path]:
    local = resource_id(metadata.resource_id).split(":", 1)[1]
    folder = Path(*_folder(metadata, context).split("/")) if _folder(metadata, context) else Path()
    root = _output(metadata, context)
    return root / "animation_clips" / folder / f"{local}.animation-clip.es", root / "animations" / folder / f"{local}.animation.es"


def _selected_frames(action, metadata, duration: int):
    start, end = frame_range(action, metadata)
    protected = required_frames(action, metadata)
    candidates = set(range(start, end + 1, metadata.sample_step)) | protected | {start, end}
    by_tick = {}
    for frame in sorted(candidates):
        tick = frame_to_tick(frame, start, end, duration)
        ideal = tick_to_frame(tick, start, end, duration)
        score = abs(frame - ideal) - (1000.0 if frame in protected else 0.0)
        if tick not in by_tick or score < by_tick[tick][0]:
            by_tick[tick] = (score, frame)
    selected = [entry[1] for _tick, entry in sorted(by_tick.items())]
    return selected, {frame: frame_to_tick(frame, start, end, duration) for frame in selected}


def build_model(context, armature, action):
    metadata = action.echo_animation
    schema = validate_rig(armature)
    fps = context.scene.render.fps / context.scene.render.fps_base
    issues = validate_structure(action, metadata, fps, armature)
    issues.extend(loop_seam_issues(context, armature, action, metadata))
    issues.extend(root_motion_issues(context, armature, action, metadata))
    store_result(metadata, issues)
    errors = [item for item in issues if item.severity == "ERROR"]
    if errors:
        raise RuntimeError(errors[0].message)
    start, end = frame_range(action, metadata)
    duration = duration_ticks(start, end, fps, metadata.engine_tick_rate, metadata.duration_ticks)
    segments = build_segments(action, metadata, duration)
    points, events = build_points_events(action, metadata, duration, segments)
    point_names = [point.name for point in points]
    duplicate_point = next((name for name in point_names if point_names.count(name) > 1), None)
    if duplicate_point:
        raise RuntimeError(f"Point name '{duplicate_point}' is used more than once; point references must be unique.")
    event_keys = [(event.name, event.tick) for event in events]
    duplicate_event = next((key for key in event_keys if event_keys.count(key) > 1), None)
    if duplicate_event:
        raise RuntimeError(f"Event '{duplicate_event[0]}' is declared twice at tick {duplicate_event[1]}.")
    frames, tick_by_frame = _selected_frames(action, metadata, duration)
    tracks, _raw = sample_tracks(context, armature, frames, tick_by_frame, metadata)
    if not tracks and metadata.channel != "FACE":
        raise RuntimeError("No animated exported bones remain after applying the selected bone mask.")
    model = ExportAnimationModel(
        resource_id=resource_id(metadata.resource_id), rig_id=resource_id(metadata.rig_id), duration_ticks=duration,
        animation_type=metadata.animation_type, channel=metadata.channel, bone_mask=metadata.bone_mask,
        source_state=sanitize_name(metadata.source_state, "idle"), active_state=sanitize_name(metadata.active_state, "active"),
        target_state=sanitize_name(metadata.target_state, "idle"), return_state=sanitize_name(metadata.return_state, "idle"),
        segments=segments, points=points, events=events, tracks=tracks,
        root_motion=RootMotion(metadata.root_motion_mode, metadata.root_bone), authored_speed=metadata.authored_speed,
        blend_in_ticks=metadata.blend_in_ticks, blend_out_ticks=metadata.blend_out_ticks,
        fallback_blend_ticks=metadata.fallback_blend_ticks, hold_last_frame=metadata.hold_last_frame,
        loop_seam=metadata.loop_seam,
    )
    return model, tuple(item for item in issues if item.severity == "WARNING")


def generated_preview(context, armature, action):
    model, warnings = build_model(context, armature, action)
    clip_path, animation_path = output_paths(action.echo_animation, context)
    return model, render_clip(model), render_animation(model), clip_path, animation_path, warnings


def export_action(context, armature, action, confirmed: bool = False) -> ExportResult:
    model, clip_text, animation_text, clip_path, animation_path, warnings = generated_preview(context, armature, action)
    targets = [(clip_path, clip_text)]
    if action.echo_animation.export_animation_resource:
        targets.append((animation_path, animation_text))
    for path, _content in targets:
        allowed, reason = overwrite_allowed(path, action.echo_animation.overwrite_policy, confirmed)
        if not allowed:
            raise RuntimeError(f"Cannot write '{path}': {reason}.")
    for path, content in targets:
        atomic_write(path, content)
    return ExportResult(str(clip_path), str(animation_path) if len(targets) > 1 else None, warnings)
