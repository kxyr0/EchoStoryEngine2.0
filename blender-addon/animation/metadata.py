from __future__ import annotations

import math

from .markers import HOLD, STATE_END, STATE_START, remove_marker, set_marker
from .naming import resource_id, sanitize_name, semantic_names
from .presets import apply as apply_preset


FIELDS = (
    "enabled", "resource_id", "rig_id", "animation_type", "preset", "channel", "bone_mask", "custom_bones",
    "source_state", "active_state", "target_state", "return_state", "enter_segment_name", "loop_segment_name",
    "exit_segment_name", "hold_point_name", "enter_enabled", "loop_enabled", "exit_enabled", "blend_in_ticks",
    "blend_out_ticks", "fallback_blend_ticks", "root_motion_mode", "root_bone", "authored_speed", "hold_last_frame",
    "export_animation_resource", "output_directory", "resource_folder", "overwrite_policy", "frame_start", "frame_end",
    "engine_tick_rate", "duration_ticks", "sample_step", "interpolation", "loop_seam", "position_tolerance",
    "rotation_tolerance", "scale_tolerance", "export_scale", "optimize_holds", "boundary_key_mode",
)


def action_range(action, fallback=(1, 30)):
    start, end = action.frame_range
    if end <= start:
        return fallback
    return math.floor(start), math.ceil(end)


def initialize(action, metadata, animation_type: str, resource: str | None = None, rig_id: str = "jdh:humanoid", frame_start: int | None = None, frame_end: int | None = None):
    default_start, default_end = action_range(action)
    metadata.enabled = True
    metadata.resource_id = resource_id(resource or action.name)
    metadata.rig_id = resource_id(rig_id)
    metadata.animation_type = animation_type
    metadata.frame_start = default_start if frame_start is None else frame_start
    metadata.frame_end = default_end if frame_end is None else frame_end
    metadata.source_state = "idle"
    metadata.active_state = sanitize_name(action.name, "active")
    metadata.target_state = "idle"
    metadata.return_state = "idle"
    metadata.enter_enabled = False
    metadata.loop_enabled = animation_type in {"LOOP", "STATEFUL_LOOP", "LOCOMOTION"}
    metadata.exit_enabled = False
    metadata.hold_last_frame = animation_type == "ONE_SHOT_HOLD"
    metadata.validation_status = "NONE"
    metadata.validation_summary = "Not validated"
    normalize_segment_names(metadata, force=True)
    sync_template_markers(action, metadata)


def normalize_segment_names(metadata, force: bool = False):
    names = semantic_names(metadata.source_state, metadata.active_state, metadata.target_state)
    if force or not metadata.enter_segment_name:
        metadata.enter_segment_name = names["enter"]
    if force or not metadata.loop_segment_name:
        metadata.loop_segment_name = names["loop"]
    if force or not metadata.exit_segment_name:
        metadata.exit_segment_name = names["exit"]


def sync_template_markers(action, metadata):
    start, end = metadata.frame_start, metadata.frame_end
    first = start + max(1, round((end - start) / 3))
    second = start + max(2, round(((end - start) * 2) / 3))
    first, second = min(first, end - 1), min(max(second, first + 1), end)
    if metadata.animation_type in {"LOOP", "LOCOMOTION"} and not metadata.enter_enabled and not metadata.exit_enabled:
        set_marker(action, STATE_START, start, "Loop Start")
        set_marker(action, STATE_END, end, "Loop End")
    elif metadata.animation_type in {"STATEFUL_LOOP", "LOCOMOTION", "ADVANCED"}:
        set_marker(action, STATE_START, first if metadata.enter_enabled else start, "Loop Start")
        set_marker(action, STATE_END, second if metadata.exit_enabled else end, "Loop End")
    else:
        remove_marker(action, STATE_START)
        remove_marker(action, STATE_END)
    if metadata.animation_type == "STATEFUL_HOLD":
        set_marker(action, HOLD, first, "Hold Pose")
    else:
        remove_marker(action, HOLD)


def snapshot(metadata) -> dict:
    result = {name: getattr(metadata, name) for name in FIELDS}
    result["markers"] = [
        {name: getattr(item, name) for name in ("marker_id", "kind", "name", "frame", "payload", "segment")}
        for item in metadata.markers
    ]
    return result


def restore(metadata, values: dict) -> None:
    for name in FIELDS:
        if name in values:
            setattr(metadata, name, values[name])
    metadata.markers.clear()
    for source in values.get("markers", ()):
        target = metadata.markers.add()
        for name, value in source.items():
            setattr(target, name, value)


def use_preset(action, metadata, preset_id: str) -> None:
    apply_preset(metadata, preset_id)
    normalize_segment_names(metadata, force=True)
    sync_template_markers(action, metadata)
