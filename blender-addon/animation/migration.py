from __future__ import annotations

import math

import bpy

from .context import active_armature
from .markers import STATE_END, STATE_START, add_semantic, set_marker
from .metadata import initialize, normalize_segment_names
from .sampling import BONE_ORDER, sample_pose
from .validation import iter_fcurves


def action_span(action) -> int:
    if action is None:
        return 0
    start, end = action.frame_range
    return max(0, math.ceil(end) - math.floor(start))


def migration_layout(enter_action, loop_action, exit_action):
    enter = action_span(enter_action)
    loop = action_span(loop_action)
    exit_span = action_span(exit_action)
    state_start = enter
    state_end = enter + loop
    return {"start": 0, "state_start": state_start, "state_end": state_end, "end": state_end + exit_span}


def _pose_at(context, armature, action, frame):
    armature.animation_data.action = action
    context.scene.frame_set(frame)
    context.view_layer.update()
    result, previous = {}, {bone: None for bone in BONE_ORDER}
    for bone in BONE_ORDER:
        result[bone], previous[bone] = sample_pose(armature, bone, previous[bone], True)
    return result


def boundary_mismatches(context, armature, left_action, right_action, tolerance=0.01):
    if left_action is None or right_action is None:
        return ()
    original_action = armature.animation_data.action
    original_frame = context.scene.frame_current
    try:
        left = _pose_at(context, armature, left_action, math.ceil(left_action.frame_range[1]))
        right = _pose_at(context, armature, right_action, math.floor(right_action.frame_range[0]))
    finally:
        armature.animation_data.action = original_action
        context.scene.frame_set(original_frame)
        context.view_layer.update()
    result = []
    for bone in BONE_ORDER:
        delta = max(abs(a - b) for group_left, group_right in zip(left[bone], right[bone]) for a, b in zip(group_left, group_right))
        if delta > tolerance:
            result.append((bone, delta))
    return tuple(result)


def _interpolation_at(action, frame):
    result = {}
    for curve in iter_fcurves(action):
        point = next((item for item in curve.keyframe_points if abs(item.co.x - frame) < 0.001), None)
        if point:
            result[(curve.data_path, curve.array_index)] = point.interpolation
    return result


def _apply_interpolation(action, frame, mapping):
    for curve in iter_fcurves(action):
        interpolation = mapping.get((curve.data_path, curve.array_index))
        if not interpolation:
            continue
        point = next((item for item in curve.keyframe_points if abs(item.co.x - frame) < 0.001), None)
        if point:
            point.interpolation = interpolation


def _bake_source(context, armature, source, target, destination_start: int, skip_first: bool):
    source_start, source_end = math.floor(source.frame_range[0]), math.ceil(source.frame_range[1])
    frames = range(source_start + (1 if skip_first else 0), source_end + 1)
    for source_frame in frames:
        armature.animation_data.action = source
        context.scene.frame_set(source_frame)
        context.view_layer.update()
        poses = {bone.name: bone.matrix_basis.copy() for bone in armature.pose.bones if bone.name in BONE_ORDER}
        interpolation = _interpolation_at(source, source_frame)
        destination_frame = destination_start + (source_frame - source_start)
        armature.animation_data.action = target
        context.scene.frame_set(destination_frame)
        for name, matrix in poses.items():
            bone = armature.pose.bones[name]
            bone.matrix_basis = matrix
            bone.keyframe_insert(data_path="location", frame=destination_frame, group=name)
            rotation_path = "rotation_quaternion" if bone.rotation_mode == "QUATERNION" else "rotation_axis_angle" if bone.rotation_mode == "AXIS_ANGLE" else "rotation_euler"
            bone.keyframe_insert(data_path=rotation_path, frame=destination_frame, group=name)
            bone.keyframe_insert(data_path="scale", frame=destination_frame, group=name)
        _apply_interpolation(target, destination_frame, interpolation)


def merge_actions(context, armature, settings):
    if settings.loop_action is None:
        raise RuntimeError("Loop Action is required for legacy migration.")
    layout = migration_layout(settings.enter_action, settings.loop_action, settings.exit_action)
    original_action = armature.animation_data_create().action
    original_frame = context.scene.frame_current
    target = bpy.data.actions.new(settings.new_name)
    target.use_fake_user = True
    succeeded = False
    try:
        destination = 0
        first = True
        for source in (settings.enter_action, settings.loop_action, settings.exit_action):
            if source is None:
                continue
            _bake_source(context, armature, source, target, destination, not first)
            destination += action_span(source)
            first = False
        armature.animation_data.action = target
        metadata = target.echo_animation
        initialize(target, metadata, "LOCOMOTION", settings.new_name, armature.data.echo_rig_id, 0, layout["end"])
        metadata.source_state, metadata.active_state, metadata.target_state = settings.source_state, settings.active_state, settings.target_state
        metadata.enter_enabled = settings.enter_action is not None
        metadata.loop_enabled = True
        metadata.exit_enabled = settings.exit_action is not None
        set_marker(target, STATE_START, layout["state_start"], "Loop Start")
        set_marker(target, STATE_END, layout["state_end"], "Loop End")
        normalize_segment_names(metadata, force=True)
        add_semantic(metadata, target, "POINT", "neutral", layout["state_end"], segment=metadata.loop_segment_name)
        succeeded = True
        return target
    except Exception:
        bpy.data.actions.remove(target)
        raise
    finally:
        if not succeeded:
            armature.animation_data.action = original_action
        context.scene.frame_set(original_frame)
        context.view_layer.update()


class ECHO_OT_preview_migration(bpy.types.Operator):
    bl_idname = "echo.preview_legacy_migration"
    bl_label = "Preview Migration"

    def execute(self, context):
        armature = active_armature(context)
        settings = context.window_manager.echo_animation_migration
        if armature is None or settings.loop_action is None:
            self.report({"ERROR"}, "Select an armature and Loop Action.")
            return {"CANCELLED"}
        layout = migration_layout(settings.enter_action, settings.loop_action, settings.exit_action)
        mismatches = list(boundary_mismatches(context, armature, settings.enter_action, settings.loop_action))
        mismatches += list(boundary_mismatches(context, armature, settings.loop_action, settings.exit_action))
        bones = ", ".join(name for name, _delta in mismatches[:6]) or "none"
        settings.preview_summary = f"Frames {layout['start']}–{layout['end']} | Enter 0–{layout['state_start']} | Loop {layout['state_start']}–{layout['state_end']} | Exit {layout['state_end']}–{layout['end']} | Boundary mismatches: {bones}"
        self.report({"WARNING"} if mismatches else {"INFO"}, settings.preview_summary)
        return {"FINISHED"}


class ECHO_OT_merge_legacy_actions(bpy.types.Operator):
    bl_idname = "echo.merge_legacy_actions"
    bl_label = "Merge to New Action"
    bl_description = "Bake legacy Enter, Loop and Exit into one new Action; source Actions are preserved"
    bl_options = {"REGISTER", "UNDO"}

    def execute(self, context):
        try:
            armature = active_armature(context)
            if armature is None:
                raise RuntimeError("Select the target armature.")
            target = merge_actions(context, armature, context.window_manager.echo_animation_migration)
            armature.animation_data.action = target
            self.report({"INFO"}, f"Created unified Action '{target.name}'. Source Actions were preserved.")
            return {"FINISHED"}
        except Exception as error:
            self.report({"ERROR"}, str(error))
            return {"CANCELLED"}


MIGRATION_CLASSES = (ECHO_OT_preview_migration, ECHO_OT_merge_legacy_actions)
