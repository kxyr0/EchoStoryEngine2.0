from __future__ import annotations

import math
import re
from pathlib import Path

from .markers import HOLD, STATE_END, STATE_START, marker_frame, sorted_echo_markers, sync_from_action
from .model import ValidationIssue
from .naming import valid_resource_id
from .sampling import BONE_ORDER, mask_bones, sample_pose
from .timeline import boundary_frames, build_segments, duration_ticks, frame_range, frame_to_tick


_BONE_PATH = re.compile(r'pose\.bones\["([^"]+)"\]')


def iter_fcurves(action):
    try:
        return tuple(action.fcurves)
    except (AttributeError, RuntimeError):
        result = []
        for layer in getattr(action, "layers", ()):
            for strip in getattr(layer, "strips", ()):
                for bag in getattr(strip, "channelbags", ()):
                    result.extend(getattr(bag, "fcurves", ()))
        return tuple(result)


def animated_bones(action) -> set[str]:
    result = set()
    for curve in iter_fcurves(action):
        match = _BONE_PATH.search(curve.data_path)
        if match:
            result.add(match.group(1))
    return result


def _issue(severity: str, code: str, message: str, bone: str = ""):
    return ValidationIssue(severity, code, message, bone)


def validate_structure(action, metadata, fps: float, armature=None) -> list[ValidationIssue]:
    issues = []
    sync_from_action(action)
    start, end = frame_range(action, metadata)
    if not metadata.enabled:
        issues.append(_issue("ERROR", "NOT_INITIALIZED", "Action is not initialized as an Echo Animation."))
    if not valid_resource_id(metadata.resource_id):
        issues.append(_issue("ERROR", "RESOURCE_ID", f"Resource ID '{metadata.resource_id}' must use namespace:path format."))
    if not valid_resource_id(metadata.rig_id):
        issues.append(_issue("ERROR", "RIG_ID", f"Rig ID '{metadata.rig_id}' must use namespace:path format."))
    if end <= start:
        issues.append(_issue("ERROR", "FRAME_RANGE", f"Action range {start}..{end} has zero or negative length."))
        return issues
    marker_ids = set()
    for marker in sorted_echo_markers(action):
        marker_id = marker.name.split(" · ", 1)[0]
        if marker_id in marker_ids:
            issues.append(_issue("ERROR", "DUPLICATE_MARKER", f"Marker '{marker_id}' exists more than once."))
        marker_ids.add(marker_id)
        if marker.frame < start or marker.frame > end:
            issues.append(_issue("ERROR", "MARKER_RANGE", f"Marker '{marker.name}' at {marker.frame} is outside {start}..{end}."))
    frames = boundary_frames(action, metadata)
    if frames["state_start"] > frames["state_end"]:
        issues.append(_issue("ERROR", "BOUNDARY_ORDER", "Loop Start must be before Loop End."))
    if metadata.animation_type == "STATEFUL_HOLD" and not start <= frames["hold"] <= end:
        issues.append(_issue("ERROR", "HOLD_RANGE", "Hold Pose lies outside the Action range."))
    needs_loop = metadata.animation_type in {"LOOP", "STATEFUL_LOOP", "LOCOMOTION"}
    if needs_loop and not metadata.loop_enabled:
        issues.append(_issue("ERROR", "LOOP_REQUIRED", f"{metadata.animation_type.replace('_', ' ').title()} requires a loop."))
    duration = duration_ticks(start, end, fps, metadata.engine_tick_rate, metadata.duration_ticks)
    try:
        segments = build_segments(action, metadata, duration)
        for segment in segments:
            if segment.end_tick <= segment.start_tick:
                issues.append(_issue("ERROR", "ZERO_SEGMENT", f"Segment '{segment.name}' maps to {segment.start_tick}..{segment.end_tick} ticks."))
        names = [segment.name for segment in segments]
        for name in set(names):
            if names.count(name) > 1:
                issues.append(_issue("ERROR", "DUPLICATE_SEGMENT", f"Segment name '{name}' is used more than once."))
    except Exception as error:
        issues.append(_issue("ERROR", "SEGMENTS", str(error)))
    if metadata.authored_speed < 0:
        issues.append(_issue("ERROR", "SPEED_NEGATIVE", "Authored speed cannot be negative."))
    if metadata.animation_type == "LOCOMOTION" and metadata.authored_speed <= 0:
        issues.append(_issue("WARNING", "SPEED_MISSING", "Locomotion has no authored speed in blocks per second."))
    if metadata.animation_type == "LOCOMOTION":
        left = sorted(item.frame for item in metadata.markers if item.kind == "LEFT_FOOT")
        right = sorted(item.frame for item in metadata.markers if item.kind == "RIGHT_FOOT")
        if not left or not right:
            issues.append(_issue("WARNING", "FOOT_CONTACTS", "Locomotion should contain at least one contact for each foot."))
        loop_start, loop_end = frames["state_start"], frames["state_end"]
        for frame in left + right:
            if frame < loop_start or frame > loop_end:
                issues.append(_issue("ERROR", "FOOT_OUTSIDE_LOOP", f"Foot contact at frame {frame} is outside loop {loop_start}..{loop_end}."))
        if set(left) & set(right):
            issues.append(_issue("WARNING", "FOOT_OVERLAP", "Left and right foot contacts overlap on the same frame."))
        order = [kind for _frame, kind in sorted([(frame, "L") for frame in left] + [(frame, "R") for frame in right])]
        if any(order[index] == order[index - 1] for index in range(1, len(order))):
            issues.append(_issue("WARNING", "FOOT_ORDER", "Foot contacts do not alternate; verify the locomotion cycle."))
        if left and loop_start in left and loop_end in left or right and loop_start in right and loop_end in right:
            issues.append(_issue("WARNING", "FOOT_BOUNDARY_DUPLICATE", "The same foot is marked at both shared loop boundaries."))
    if metadata.root_motion_mode != "DISABLED" and metadata.root_bone not in BONE_ORDER:
        issues.append(_issue("ERROR", "ROOT_BONE", f"Root motion bone '{metadata.root_bone}' is not exported by this rig."))
    output = Path(metadata.output_directory) if metadata.output_directory else None
    if output and output.exists() and not output.is_dir():
        issues.append(_issue("ERROR", "OUTPUT_PATH", f"Output path '{output}' is not a directory."))
    animated, allowed = animated_bones(action), set(mask_bones(metadata))
    unknown = animated - set(BONE_ORDER)
    for bone in sorted(unknown):
        issues.append(_issue("WARNING", "UNKNOWN_BONE", f"Animated control or unknown bone '{bone}' will not be exported.", bone))
    for bone in sorted((animated & set(BONE_ORDER)) - allowed):
        issues.append(_issue("WARNING", "OUTSIDE_MASK", f"Animated bone '{bone}' is outside the {metadata.channel} mask.", bone))
    if metadata.channel == "UPPER_BODY" and animated & {"left_thigh", "left_shin", "right_thigh", "right_shin"}:
        issues.append(_issue("WARNING", "UPPER_BODY_LEGS", "Upper-body animation contains leg keys."))
    curves = iter_fcurves(action)
    if any(curve.modifiers for curve in curves):
        issues.append(_issue("WARNING", "FCURVE_MODIFIERS", "Action contains F-Curve modifiers; export uses evaluated poses."))
    keyed_frames = [point.co.x for curve in curves for point in curve.keyframe_points]
    if any(frame < start or frame > end for frame in keyed_frames):
        issues.append(_issue("WARNING", "KEYS_OUTSIDE", "Action contains keys outside the configured export range."))
    boundaries = {frames["start"], frames["end"]}
    if metadata.enter_enabled or metadata.loop_enabled:
        boundaries.add(frames["state_start"])
    if metadata.exit_enabled or metadata.loop_enabled:
        boundaries.add(frames["state_end"])
    missing = sorted(frame for frame in boundaries if not any(abs(key - frame) < 0.001 for key in keyed_frames))
    if missing and keyed_frames:
        issues.append(_issue("WARNING", "BOUNDARY_KEYS", "No explicit key exists at boundary frames: " + ", ".join(map(str, missing)) + "."))
    if any(curve.data_path.endswith("scale") for curve in curves):
        issues.append(_issue("INFO", "SCALE_TRACKS", "Action contains scale curves; verify that the target rig expects animated scale."))
    if abs(fps - metadata.engine_tick_rate) > 0.001:
        issues.append(_issue("INFO", "FRAME_RATE", f"Blender {fps:.3f} FPS is converted to {metadata.engine_tick_rate:.3f} engine ticks/s across the complete timeline."))
    mapped = [frame_to_tick(frame, start, end, duration) for frame in range(start, end + 1)]
    if len(mapped) != len(set(mapped)):
        issues.append(_issue("WARNING", "TICK_COLLISION", "Multiple Blender frames map to the same engine tick; boundary frames take priority."))
    return issues


def loop_seam_issues(context, armature, action, metadata) -> list[ValidationIssue]:
    if metadata.animation_type not in {"LOOP", "STATEFUL_LOOP", "LOCOMOTION", "ADVANCED"} or not metadata.loop_enabled:
        return []
    start = marker_frame(action, STATE_START, frame_range(action, metadata)[0])
    end = marker_frame(action, STATE_END, frame_range(action, metadata)[1])
    original_frame = context.scene.frame_current
    previous = {bone: None for bone in mask_bones(metadata)}
    sampled = {}
    try:
        for frame in (start, end):
            context.scene.frame_set(frame)
            context.view_layer.update()
            sampled[frame] = {}
            for bone in previous:
                sampled[frame][bone], previous[bone] = sample_pose(armature, bone, previous[bone], metadata.export_scale)
    finally:
        context.scene.frame_set(original_frame)
        context.view_layer.update()
    issues = []
    for bone in previous:
        first, last = sampled[start][bone], sampled[end][bone]
        position = max(abs(a - b) for a, b in zip(first[0], last[0]))
        rotation = max(abs(((a - b + 180.0) % 360.0) - 180.0) for a, b in zip(first[1], last[1]))
        scale = max(abs(a - b) for a, b in zip(first[2], last[2]))
        if position > metadata.position_tolerance or rotation > metadata.rotation_tolerance or scale > metadata.scale_tolerance:
            issues.append(_issue("ERROR", "LOOP_SEAM", f"Loop seam mismatch on '{bone}': position {position:.5f}, rotation {rotation:.2f}°, scale {scale:.5f} at frames {start}/{end}.", bone))
    return issues


def root_motion_issues(context, armature, action, metadata) -> list[ValidationIssue]:
    if metadata.root_bone not in armature.pose.bones:
        return []
    start, end = frame_range(action, metadata)
    original = context.scene.frame_current
    values, previous = [], None
    try:
        for frame in (start, end):
            context.scene.frame_set(frame)
            context.view_layer.update()
            value, previous = sample_pose(armature, metadata.root_bone, previous, metadata.export_scale)
            values.append(value)
    finally:
        context.scene.frame_set(original)
        context.view_layer.update()
    distance = math.dist(values[0][0], values[1][0])
    if metadata.root_motion_mode == "DISABLED" and distance > metadata.position_tolerance:
        return [_issue("WARNING", "ROOT_DRIFT", f"Root bone drifts {distance:.5f} model units while Root Motion is disabled.", metadata.root_bone)]
    if metadata.root_motion_mode == "PRESERVE":
        return [_issue("WARNING", "ROOT_PRESERVE", "Preserved root motion requires a runtime locomotion owner that accepts bone-driven displacement.", metadata.root_bone)]
    return []


def store_result(metadata, issues: list[ValidationIssue]) -> None:
    errors = sum(item.severity == "ERROR" for item in issues)
    warnings = sum(item.severity == "WARNING" for item in issues)
    metadata.validation_status = "INVALID" if errors else "WARNING" if warnings else "VALID"
    metadata.validation_summary = f"{errors} errors, {warnings} warnings"
    metadata.validation_details = "\n".join(f"[{item.severity}] {item.message}" for item in issues) or "Animation is valid."
