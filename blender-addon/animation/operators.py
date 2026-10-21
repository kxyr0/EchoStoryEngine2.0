from __future__ import annotations

import math
import bpy
from bpy.props import BoolProperty, EnumProperty, IntProperty, StringProperty

from .context import active_action, active_armature
from .markers import HOLD, ITEM_MARKERS, STATE_END, STATE_START, add_item_marker, add_semantic, find, marker_frame, remove_duplicates, remove_semantic, set_marker, sorted_echo_markers
from .metadata import initialize, normalize_segment_names, restore, snapshot, sync_template_markers, use_preset
from .naming import resource_id, sanitize_name
from .properties import ANIMATION_TYPES
from .sampling import mask_bones, sample_pose
from .validation import iter_fcurves, loop_seam_issues, root_motion_issues, store_result, validate_structure


SETTINGS_CLIPBOARD = None


def _require(context):
    armature, action = active_armature(context), active_action(context)
    if armature is None or action is None:
        raise RuntimeError("Select an armature with an active Action.")
    return armature, action, action.echo_animation


class ECHO_OT_copy_text(bpy.types.Operator):
    bl_idname = "echo.copy_text"
    bl_label = "Copy"
    bl_options = {"INTERNAL"}
    text: StringProperty(default="")

    def execute(self, context):
        context.window_manager.clipboard = self.text
        return {"FINISHED"}


class ECHO_OT_create_animation(bpy.types.Operator):
    bl_idname = "echo.create_animation"
    bl_label = "Create Animation"
    bl_description = "Create and attach one Action containing the complete Echo animation timeline"
    bl_options = {"REGISTER", "UNDO"}
    action_name: StringProperty(name="Action", default="new_animation")
    resource: StringProperty(name="Resource ID", default="jdh:new_animation")
    rig_id: StringProperty(name="Rig", default="jdh:humanoid")
    animation_type: EnumProperty(name="Animation Type", items=ANIMATION_TYPES, default="ONE_SHOT")
    frame_start: IntProperty(name="Start", default=1)
    frame_end: IntProperty(name="End", default=30)

    @classmethod
    def poll(cls, context):
        return active_armature(context) is not None

    def invoke(self, context, _event):
        draft = context.window_manager.echo_animation_draft
        self.action_name, self.resource, self.rig_id = draft.action_name, draft.resource_id, draft.rig_id
        self.animation_type, self.frame_start, self.frame_end = draft.animation_type, draft.frame_start, draft.frame_end
        return context.window_manager.invoke_props_dialog(self, width=460)

    def execute(self, context):
        armature = active_armature(context)
        if armature is None or self.frame_end <= self.frame_start:
            self.report({"ERROR"}, "Select an armature and use a positive frame range.")
            return {"CANCELLED"}
        name = sanitize_name(self.action_name, "new_animation")
        action = bpy.data.actions.new(name=name)
        action.use_fake_user = True
        armature.animation_data_create().action = action
        initialize(action, action.echo_animation, self.animation_type, self.resource, self.rig_id, self.frame_start, self.frame_end)
        draft = context.window_manager.echo_animation_draft
        draft.action_name, draft.resource_id = name, resource_id(self.resource)
        self.report({"INFO"}, f"Created Echo Action '{name}'.")
        return {"FINISHED"}


class ECHO_OT_initialize_action(bpy.types.Operator):
    bl_idname = "echo.initialize_action"
    bl_label = "Initialize Existing Action"
    bl_description = "Attach Action-local Echo metadata without changing existing keys"
    bl_options = {"REGISTER", "UNDO"}

    def execute(self, context):
        try:
            armature, action, metadata = _require(context)
            initialize(action, metadata, context.window_manager.echo_animation_draft.animation_type, action.name, armature.data.echo_rig_id)
            self.report({"INFO"}, f"Initialized '{action.name}'.")
            return {"FINISHED"}
        except Exception as error:
            self.report({"ERROR"}, str(error))
            return {"CANCELLED"}


class ECHO_OT_activate_action(bpy.types.Operator):
    bl_idname = "echo.activate_animation_action"
    bl_label = "Activate Action"
    action_name: StringProperty(default="")

    def execute(self, context):
        armature = active_armature(context)
        action = bpy.data.actions.get(self.action_name)
        if armature is None or action is None:
            return {"CANCELLED"}
        armature.animation_data_create().action = action
        return {"FINISHED"}


class ECHO_OT_rename_animation(bpy.types.Operator):
    bl_idname = "echo.rename_animation"
    bl_label = "Rename Action and Resource Together"
    bl_options = {"REGISTER", "UNDO"}
    new_name: StringProperty(name="New Name", default="animation")
    update_resource: BoolProperty(name="Update Resource ID", default=True)
    update_segments: BoolProperty(name="Regenerate Segment Names", default=True)

    def invoke(self, context, _event):
        action = active_action(context)
        self.new_name = action.name if action else "animation"
        return context.window_manager.invoke_props_dialog(self)

    def execute(self, context):
        try:
            _armature, action, metadata = _require(context)
            action.name = sanitize_name(self.new_name)
            if self.update_resource:
                metadata.resource_id = resource_id(action.name)
            if self.update_segments:
                normalize_segment_names(metadata, force=True)
            return {"FINISHED"}
        except Exception as error:
            self.report({"ERROR"}, str(error))
            return {"CANCELLED"}


class ECHO_OT_apply_preset(bpy.types.Operator):
    bl_idname = "echo.apply_animation_preset"
    bl_label = "Apply Preset"
    bl_description = "Fill metadata and timeline boundaries without creating bone animation"
    bl_options = {"REGISTER", "UNDO"}

    def execute(self, context):
        try:
            _armature, action, metadata = _require(context)
            use_preset(action, metadata, metadata.preset)
            return {"FINISHED"}
        except Exception as error:
            self.report({"ERROR"}, str(error))
            return {"CANCELLED"}


class ECHO_OT_set_boundary(bpy.types.Operator):
    bl_idname = "echo.set_animation_boundary"
    bl_label = "Set Animation Boundary"
    bl_description = "Place the selected Action-local boundary at the current frame"
    bl_options = {"REGISTER", "UNDO"}
    boundary: EnumProperty(items=(("START", "Loop Start", "Shared Enter/Loop boundary"), ("END", "Loop End", "Shared Loop/Exit boundary"), ("HOLD", "Hold Pose", "Frame held by a stateful hold")))

    def execute(self, context):
        try:
            _armature, action, metadata = _require(context)
            marker_id, label = {"START": (STATE_START, "Loop Start"), "END": (STATE_END, "Loop End"), "HOLD": (HOLD, "Hold Pose")}[self.boundary]
            set_marker(action, marker_id, context.scene.frame_current, label)
            if self.boundary == "START":
                metadata.enter_enabled = context.scene.frame_current > metadata.frame_start
            elif self.boundary == "END":
                metadata.exit_enabled = context.scene.frame_current < metadata.frame_end
            return {"FINISHED"}
        except Exception as error:
            self.report({"ERROR"}, str(error))
            return {"CANCELLED"}


class ECHO_OT_use_full_loop(bpy.types.Operator):
    bl_idname = "echo.use_full_action_loop"
    bl_label = "Use Full Action as Loop"
    bl_options = {"REGISTER", "UNDO"}

    def execute(self, context):
        try:
            _armature, action, metadata = _require(context)
            metadata.loop_enabled, metadata.enter_enabled, metadata.exit_enabled = True, False, False
            set_marker(action, STATE_START, metadata.frame_start, "Loop Start")
            set_marker(action, STATE_END, metadata.frame_end, "Loop End")
            return {"FINISHED"}
        except Exception as error:
            self.report({"ERROR"}, str(error))
            return {"CANCELLED"}


class ECHO_OT_add_marker_item(bpy.types.Operator):
    bl_idname = "echo.add_animation_marker"
    bl_label = "Add Event or Point"
    bl_options = {"REGISTER", "UNDO"}
    kind: EnumProperty(items=(("EVENT", "Event", "One-time event"), ("POINT", "Point", "Safe transition point"), ("LEFT_FOOT", "Left Foot", "Left foot contact"), ("RIGHT_FOOT", "Right Foot", "Right foot contact")))
    name: StringProperty(name="Name", default="point")
    payload: StringProperty(name="Payload", default="")
    segment: StringProperty(name="Segment", default="")

    def invoke(self, context, _event):
        if self.kind == "LEFT_FOOT":
            self.name = "left_contact"
        elif self.kind == "RIGHT_FOOT":
            self.name = "right_contact"
        return context.window_manager.invoke_props_dialog(self, width=420) if self.kind in {"EVENT", "POINT"} else self.execute(context)

    def execute(self, context):
        try:
            _armature, action, metadata = _require(context)
            add_semantic(metadata, action, self.kind, sanitize_name(self.name), context.scene.frame_current, self.payload, self.segment)
            return {"FINISHED"}
        except Exception as error:
            self.report({"ERROR"}, str(error))
            return {"CANCELLED"}


class ECHO_OT_add_item_attachment_marker(bpy.types.Operator):
    bl_idname = "echo.add_item_attachment_marker"
    bl_label = "Set Item Attachment Marker"
    bl_options = {"REGISTER", "UNDO"}
    marker: EnumProperty(items=tuple((name, name, f"Set exact {name} marker") for name in ITEM_MARKERS))

    def execute(self, context):
        try:
            _armature, action, metadata = _require(context)
            add_item_marker(metadata, action, self.marker, context.scene.frame_current)
            return {"FINISHED"}
        except Exception as error:
            self.report({"ERROR"}, str(error))
            return {"CANCELLED"}


class ECHO_OT_marker_action(bpy.types.Operator):
    bl_idname = "echo.animation_marker_action"
    bl_label = "Edit Echo Marker"
    bl_options = {"REGISTER", "UNDO"}
    operation: EnumProperty(items=(("SET", "Set Here", "Move to current frame"), ("JUMP", "Jump To", "Jump to marker"), ("DELETE", "Delete", "Delete marker")))
    index: IntProperty(default=-1)

    def execute(self, context):
        try:
            _armature, action, metadata = _require(context)
            if self.index < 0 or self.index >= len(metadata.markers):
                raise RuntimeError("Marker no longer exists.")
            item = metadata.markers[self.index]
            marker = find(action, item.marker_id)
            if self.operation == "DELETE":
                remove_semantic(metadata, action, self.index)
            elif marker is None:
                raise RuntimeError("Timeline marker is missing; run validation or recreate it.")
            elif self.operation == "SET":
                marker.frame = item.frame = context.scene.frame_current
            else:
                context.scene.frame_set(marker.frame)
            return {"FINISHED"}
        except Exception as error:
            self.report({"ERROR"}, str(error))
            return {"CANCELLED"}


class ECHO_OT_jump_echo_marker(bpy.types.Operator):
    bl_idname = "echo.jump_echo_marker"
    bl_label = "Jump to Echo Marker"
    direction: EnumProperty(items=(("PREVIOUS", "Previous", "Previous Echo marker"), ("NEXT", "Next", "Next Echo marker")))

    def execute(self, context):
        try:
            _armature, action, _metadata = _require(context)
            markers = sorted_echo_markers(action)
            current = context.scene.frame_current
            candidates = [m for m in markers if m.frame < current] if self.direction == "PREVIOUS" else [m for m in markers if m.frame > current]
            if candidates:
                context.scene.frame_set(candidates[-1].frame if self.direction == "PREVIOUS" else candidates[0].frame)
            return {"FINISHED"}
        except Exception as error:
            self.report({"ERROR"}, str(error))
            return {"CANCELLED"}


class ECHO_OT_validate_animation(bpy.types.Operator):
    bl_idname = "echo.validate_animation"
    bl_label = "Validate Animation"
    bl_description = "Check timeline, masks, frame conversion and loop seam, including individual problem bones"

    include_seam: BoolProperty(default=True)

    def execute(self, context):
        try:
            armature, action, metadata = _require(context)
            fps = context.scene.render.fps / context.scene.render.fps_base
            issues = validate_structure(action, metadata, fps, armature)
            if self.include_seam:
                issues.extend(loop_seam_issues(context, armature, action, metadata))
            issues.extend(root_motion_issues(context, armature, action, metadata))
            store_result(metadata, issues)
            self.report({"ERROR"} if metadata.validation_status == "INVALID" else {"WARNING"} if metadata.validation_status == "WARNING" else {"INFO"}, metadata.validation_summary)
            return {"FINISHED"}
        except Exception as error:
            self.report({"ERROR"}, str(error))
            return {"CANCELLED"}


class ECHO_OT_select_problem_bones(bpy.types.Operator):
    bl_idname = "echo.select_problem_bones"
    bl_label = "Select Problem Bones"

    def execute(self, context):
        try:
            armature, _action, metadata = _require(context)
            names = {line.split("'")[1] for line in metadata.validation_details.splitlines() if "'" in line and "Loop seam" in line}
            for bone in armature.data.bones:
                bone.select = bone.name in names
            self.report({"INFO"}, f"Selected {len(names)} problem bones.")
            return {"FINISHED"}
        except Exception as error:
            self.report({"ERROR"}, str(error))
            return {"CANCELLED"}


class ECHO_OT_animation_fix(bpy.types.Operator):
    bl_idname = "echo.animation_fix"
    bl_label = "Animation Fix"
    bl_options = {"REGISTER", "UNDO"}
    fix: EnumProperty(items=(("RANGE", "Set Range from Keys", "Use key range"), ("CLAMP", "Clamp Markers", "Clamp markers to range"), ("NAMES", "Normalize Names", "Generate semantic segment names"), ("DUPLICATES", "Remove Duplicate Markers", "Remove duplicate service markers")))

    def execute(self, context):
        try:
            _armature, action, metadata = _require(context)
            if self.fix == "RANGE":
                frames = [point.co.x for curve in iter_fcurves(action) for point in curve.keyframe_points]
                if frames:
                    metadata.frame_start, metadata.frame_end = int(min(frames)), int(max(frames))
            elif self.fix == "CLAMP":
                for marker in sorted_echo_markers(action):
                    marker.frame = min(metadata.frame_end, max(metadata.frame_start, marker.frame))
            elif self.fix == "NAMES":
                normalize_segment_names(metadata, force=True)
            else:
                removed = remove_duplicates(action)
                self.report({"INFO"}, f"Removed {removed} duplicate markers.")
            return {"FINISHED"}
        except Exception as error:
            self.report({"ERROR"}, str(error))
            return {"CANCELLED"}


class ECHO_OT_create_boundary_keys(bpy.types.Operator):
    bl_idname = "echo.create_boundary_keys"
    bl_label = "Create Boundary Keys"
    bl_description = "Insert evaluated pose keys at every configured shared segment boundary"
    bl_options = {"REGISTER", "UNDO"}

    def execute(self, context):
        try:
            armature, action, metadata = _require(context)
            frames = {metadata.frame_start, metadata.frame_end}
            frames.update(value for value in (marker_frame(action, STATE_START), marker_frame(action, STATE_END), marker_frame(action, HOLD)) if value is not None)
            animated = {match.group(1) for curve in iter_fcurves(action) if (match := __import__('re').search(r'pose\.bones\["([^"]+)"\]', curve.data_path))}
            bones = mask_bones(metadata)
            if metadata.boundary_key_mode == "ANIMATED":
                bones = tuple(bone for bone in bones if bone in animated)
            original = context.scene.frame_current
            try:
                for frame in sorted(frames):
                    context.scene.frame_set(frame)
                    context.view_layer.update()
                    for name in bones:
                        bone = armature.pose.bones.get(name)
                        if bone:
                            for path in ("location", "rotation_euler", "rotation_quaternion", "scale"):
                                if path.startswith("rotation_") and bone.rotation_mode not in ({"QUATERNION"} if path.endswith("quaternion") else {"XYZ", "XZY", "YXZ", "YZX", "ZXY", "ZYX"}):
                                    continue
                                bone.keyframe_insert(data_path=path, frame=frame, group=bone.name)
            finally:
                context.scene.frame_set(original)
                context.view_layer.update()
            self.report({"INFO"}, f"Created boundary keys on {len(bones)} bones at {len(frames)} frames.")
            return {"FINISHED"}
        except Exception as error:
            self.report({"ERROR"}, str(error))
            return {"CANCELLED"}


class ECHO_OT_copy_loop_pose(bpy.types.Operator):
    bl_idname = "echo.copy_loop_start_pose"
    bl_label = "Copy Loop Start Pose to Loop End"
    bl_description = "Destructively replace the loop-end pose and keys with the evaluated loop-start pose"
    bl_options = {"REGISTER", "UNDO"}

    def invoke(self, context, event):
        return context.window_manager.invoke_confirm(self, event)

    def execute(self, context):
        try:
            armature, action, metadata = _require(context)
            start = marker_frame(action, STATE_START, metadata.frame_start)
            end = marker_frame(action, STATE_END, metadata.frame_end)
            original = context.scene.frame_current
            try:
                context.scene.frame_set(start)
                context.view_layer.update()
                poses = {name: armature.pose.bones[name].matrix_basis.copy() for name in mask_bones(metadata)}
                context.scene.frame_set(end)
                for name, matrix in poses.items():
                    bone = armature.pose.bones[name]
                    bone.matrix_basis = matrix
                    bone.keyframe_insert(data_path="location", frame=end, group=name)
                    rotation_path = "rotation_quaternion" if bone.rotation_mode == "QUATERNION" else "rotation_axis_angle" if bone.rotation_mode == "AXIS_ANGLE" else "rotation_euler"
                    bone.keyframe_insert(data_path=rotation_path, frame=end, group=name)
                    bone.keyframe_insert(data_path="scale", frame=end, group=name)
            finally:
                context.scene.frame_set(original)
                context.view_layer.update()
            return {"FINISHED"}
        except Exception as error:
            self.report({"ERROR"}, str(error))
            return {"CANCELLED"}


class ECHO_OT_calculate_authored_speed(bpy.types.Operator):
    bl_idname = "echo.calculate_authored_speed"
    bl_label = "Calculate from Root Motion"
    bl_description = "Calculate blocks per second from root displacement across the loop without editing the Action"

    def execute(self, context):
        try:
            armature, action, metadata = _require(context)
            start = marker_frame(action, STATE_START, metadata.frame_start)
            end = marker_frame(action, STATE_END, metadata.frame_end)
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
            distance_units = math.dist(values[0][0], values[1][0])
            fps = context.scene.render.fps / context.scene.render.fps_base
            seconds = (end - start) / fps
            if seconds <= 0 or distance_units <= 1e-6:
                raise RuntimeError("Root motion has no measurable displacement across the loop.")
            metadata.authored_speed = (distance_units / 16.0) / seconds
            self.report({"INFO"}, f"Authored speed: {metadata.authored_speed:.3f} blocks/s.")
            return {"FINISHED"}
        except Exception as error:
            self.report({"ERROR"}, str(error))
            return {"CANCELLED"}


class ECHO_OT_settings_clipboard(bpy.types.Operator):
    bl_idname = "echo.animation_settings_clipboard"
    bl_label = "Copy/Paste Animation Settings"
    bl_options = {"REGISTER", "UNDO"}
    operation: EnumProperty(items=(("COPY", "Copy", "Copy metadata"), ("PASTE", "Paste", "Paste metadata")))

    def execute(self, context):
        global SETTINGS_CLIPBOARD
        try:
            _armature, action, metadata = _require(context)
            if self.operation == "COPY":
                SETTINGS_CLIPBOARD = snapshot(metadata)
            elif SETTINGS_CLIPBOARD is None:
                raise RuntimeError("No Echo settings have been copied.")
            else:
                restore(metadata, SETTINGS_CLIPBOARD)
                sync_template_markers(action, metadata)
            return {"FINISHED"}
        except Exception as error:
            self.report({"ERROR"}, str(error))
            return {"CANCELLED"}


class ECHO_OT_duplicate_animation(bpy.types.Operator):
    bl_idname = "echo.duplicate_animation"
    bl_label = "Duplicate as New Echo Animation"
    bl_options = {"REGISTER", "UNDO"}
    new_name: StringProperty(name="New Name", default="animation_copy")

    def invoke(self, context, _event):
        action = active_action(context)
        self.new_name = sanitize_name((action.name if action else "animation") + "_copy")
        return context.window_manager.invoke_props_dialog(self)

    def execute(self, context):
        try:
            armature, action, _metadata = _require(context)
            duplicate = action.copy()
            duplicate.name = sanitize_name(self.new_name)
            duplicate.use_fake_user = True
            duplicate.echo_animation.resource_id = resource_id(duplicate.name)
            armature.animation_data.action = duplicate
            return {"FINISHED"}
        except Exception as error:
            self.report({"ERROR"}, str(error))
            return {"CANCELLED"}


class ECHO_OT_reset_metadata(bpy.types.Operator):
    bl_idname = "echo.reset_animation_metadata"
    bl_label = "Reset Echo Metadata"
    bl_options = {"REGISTER", "UNDO"}

    def invoke(self, context, _event):
        return context.window_manager.invoke_confirm(self, _event)

    def execute(self, context):
        try:
            armature, action, metadata = _require(context)
            for marker in list(action.pose_markers):
                if marker.name.startswith("ECHO_"):
                    action.pose_markers.remove(marker)
            initialize(action, metadata, "ONE_SHOT", action.name, armature.data.echo_rig_id)
            return {"FINISHED"}
        except Exception as error:
            self.report({"ERROR"}, str(error))
            return {"CANCELLED"}


OPERATOR_CLASSES = (
    ECHO_OT_copy_text, ECHO_OT_create_animation, ECHO_OT_initialize_action, ECHO_OT_activate_action,
    ECHO_OT_rename_animation, ECHO_OT_apply_preset,
    ECHO_OT_set_boundary, ECHO_OT_use_full_loop, ECHO_OT_add_marker_item,
    ECHO_OT_add_item_attachment_marker, ECHO_OT_marker_action,
    ECHO_OT_jump_echo_marker, ECHO_OT_validate_animation, ECHO_OT_select_problem_bones,
    ECHO_OT_animation_fix, ECHO_OT_create_boundary_keys, ECHO_OT_copy_loop_pose,
    ECHO_OT_calculate_authored_speed, ECHO_OT_settings_clipboard,
    ECHO_OT_duplicate_animation, ECHO_OT_reset_metadata,
)
