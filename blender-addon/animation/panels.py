from __future__ import annotations

import bpy

from .context import active_action, active_armature
from .export_operators import LAST_BATCH, PREVIEW_CACHE
from .markers import HOLD, STATE_END, STATE_START, marker_frame, sync_from_action
from .timeline import duration_ticks, frame_range


def _meta(context):
    action = active_action(context)
    return action, action.echo_animation if action else None


class EchoPanelBase:
    bl_space_type = "DOPESHEET_EDITOR"
    bl_region_type = "UI"
    bl_category = "Echo"


class ECHO_PT_animation_root(EchoPanelBase, bpy.types.Panel):
    bl_label = "Echo Animation"
    bl_idname = "ECHO_PT_animation_root"

    def draw(self, context):
        layout = self.layout
        armature, action = active_armature(context), active_action(context)
        if armature is None:
            layout.label(text="Select an Armature", icon="ARMATURE_DATA")
            return
        layout.label(text=f"Armature: {armature.name}", icon="ARMATURE_DATA")
        layout.label(text=f"Action: {action.name if action else 'None'}", icon="ACTION")
        action_box = layout.box()
        action_box.label(text="Echo Actions")
        for candidate in list(bpy.data.actions)[:8]:
            candidate_meta = candidate.echo_animation
            icon = {"VALID": "CHECKMARK", "WARNING": "ERROR", "INVALID": "CANCEL", "NONE": "DOT"}.get(candidate_meta.validation_status, "DOT")
            row = action_box.row(align=True)
            row.label(text=candidate.name, icon=icon)
            activate = row.operator("echo.activate_animation_action", text="", icon="RESTRICT_SELECT_OFF")
            activate.action_name = candidate.name
        if action is None:
            layout.operator("echo.create_animation", icon="ADD")
            return
        if not action.echo_animation.enabled:
            row = layout.row(align=True)
            row.operator("echo.initialize_action", icon="LINKED")
            row.operator("echo.create_animation", icon="ADD")
            return
        status_icon = {"VALID": "CHECKMARK", "WARNING": "ERROR", "INVALID": "CANCEL", "NONE": "QUESTION"}[action.echo_animation.validation_status]
        layout.label(text=action.echo_animation.validation_summary, icon=status_icon)


class ECHO_PT_animation_settings(EchoPanelBase, bpy.types.Panel):
    bl_label = "Animation"
    bl_parent_id = "ECHO_PT_animation_root"
    bl_options = {"DEFAULT_CLOSED"}

    @classmethod
    def poll(cls, context):
        action = active_action(context)
        return action is not None and action.echo_animation.enabled

    def draw(self, context):
        layout, action = self.layout, active_action(context)
        meta = action.echo_animation
        layout.prop(meta, "resource_id")
        layout.prop(meta, "rig_id")
        layout.prop(meta, "animation_type")
        row = layout.row(align=True)
        row.prop(meta, "preset")
        row.operator("echo.apply_animation_preset", text="Apply")
        layout.prop(meta, "channel")
        if meta.channel == "CUSTOM":
            layout.prop(meta, "custom_bones")
        if meta.animation_type in {"STATEFUL_LOOP", "STATEFUL_HOLD", "LOCOMOTION", "ADVANCED"}:
            layout.prop(meta, "source_state")
            layout.prop(meta, "active_state")
            layout.prop(meta, "target_state")
        if meta.animation_type in {"ONE_SHOT", "ONE_SHOT_HOLD"}:
            layout.prop(meta, "return_state")
            layout.prop(meta, "hold_last_frame")
        if meta.animation_type == "LOCOMOTION":
            layout.prop(meta, "authored_speed")
            layout.operator("echo.calculate_authored_speed", icon="DRIVER_DISTANCE")
            layout.prop(meta, "root_motion_mode")
            if meta.root_motion_mode != "DISABLED":
                layout.prop(meta, "root_bone")
        row = layout.row(align=True)
        row.operator("echo.duplicate_animation", icon="DUPLICATE")
        row.operator("echo.rename_animation", text="Rename", icon="GREASEPENCIL")
        row.operator("echo.reset_animation_metadata", text="Reset", icon="LOOP_BACK")
        row = layout.row(align=True)
        copy = row.operator("echo.animation_settings_clipboard", text="Copy Settings")
        copy.operation = "COPY"
        paste = row.operator("echo.animation_settings_clipboard", text="Paste Settings")
        paste.operation = "PASTE"


class ECHO_PT_animation_timeline(EchoPanelBase, bpy.types.Panel):
    bl_label = "Timeline"
    bl_parent_id = "ECHO_PT_animation_root"

    @classmethod
    def poll(cls, context):
        action = active_action(context)
        return action is not None and action.echo_animation.enabled

    def draw(self, context):
        layout, action = self.layout, active_action(context)
        meta = action.echo_animation
        sync_from_action(action)
        row = layout.row(align=True)
        row.prop(meta, "frame_start")
        row.prop(meta, "frame_end")
        fps = context.scene.render.fps / context.scene.render.fps_base
        start, end = frame_range(action, meta)
        ticks = duration_ticks(start, end, fps, meta.engine_tick_rate, meta.duration_ticks)
        layout.label(text=f"Blender: {fps:.3f} FPS | Engine: {meta.engine_tick_rate:.3f} ticks/s")
        layout.label(text=f"Frames/tick: {fps / meta.engine_tick_rate:.3f} | Export: {ticks} ticks")
        row = layout.row(align=True)
        row.prop(meta, "sample_step")
        row.prop(meta, "duration_ticks")
        row = layout.row(align=True)
        row.operator("echo.jump_echo_marker", text="Previous", icon="TRIA_LEFT").direction = "PREVIOUS"
        row.operator("echo.jump_echo_marker", text="Next", icon="TRIA_RIGHT").direction = "NEXT"


class ECHO_PT_animation_segments(EchoPanelBase, bpy.types.Panel):
    bl_label = "Segments"
    bl_parent_id = "ECHO_PT_animation_root"

    @classmethod
    def poll(cls, context):
        action = active_action(context)
        return action is not None and action.echo_animation.enabled

    def draw(self, context):
        layout, action = self.layout, active_action(context)
        meta = action.echo_animation
        if meta.animation_type in {"LOOP", "STATEFUL_LOOP", "LOCOMOTION", "ADVANCED"}:
            layout.prop(meta, "loop_enabled")
            row = layout.row(align=True)
            row.operator("echo.use_full_action_loop", icon="PREVIEW_RANGE")
            start = row.operator("echo.set_animation_boundary", text="Set Loop Start")
            start.boundary = "START"
            end = row.operator("echo.set_animation_boundary", text="Set Loop End")
            end.boundary = "END"
            layout.label(text=f"Loop: {marker_frame(action, STATE_START, meta.frame_start)} to {marker_frame(action, STATE_END, meta.frame_end)}")
        if meta.animation_type in {"STATEFUL_LOOP", "LOCOMOTION", "ADVANCED"}:
            row = layout.row(align=True)
            row.prop(meta, "enter_enabled")
            row.prop(meta, "exit_enabled")
        if meta.animation_type == "STATEFUL_HOLD":
            hold = layout.operator("echo.set_animation_boundary", text="Use Current Frame as Hold Pose")
            hold.boundary = "HOLD"
            layout.label(text=f"Hold frame: {marker_frame(action, HOLD, meta.frame_start)}")
        if meta.animation_type == "ADVANCED":
            layout.prop(meta, "enter_segment_name")
            layout.prop(meta, "loop_segment_name")
            layout.prop(meta, "exit_segment_name")
        row = layout.row(align=True)
        row.operator("echo.create_boundary_keys", icon="KEY_HLT")
        row.prop(meta, "boundary_key_mode", text="")
        if meta.loop_enabled:
            layout.operator("echo.copy_loop_start_pose", icon="PASTEDOWN")


class ECHO_PT_animation_events(EchoPanelBase, bpy.types.Panel):
    bl_label = "Events and Points"
    bl_parent_id = "ECHO_PT_animation_root"
    bl_options = {"DEFAULT_CLOSED"}

    @classmethod
    def poll(cls, context):
        action = active_action(context)
        return action is not None and action.echo_animation.enabled

    def draw(self, context):
        layout, action = self.layout, active_action(context)
        meta = action.echo_animation
        sync_from_action(action)
        row = layout.row(align=True)
        row.operator("echo.add_animation_marker", text="Add Event").kind = "EVENT"
        row.operator("echo.add_animation_marker", text="Add Point").kind = "POINT"
        layout.label(text="Item custody markers (exact IDs):")
        row = layout.row(align=True)
        for marker, label in (("echo_item_attachments:attach_left_hand", "Attach Left"),
                              ("echo_item_attachments:attach_right_hand", "Attach Right"),
                              ("echo_item_attachments:release", "Release"),
                              ("echo_item_attachments:detach", "Detach")):
            button = row.operator("echo.add_item_attachment_marker", text=label)
            button.marker = marker
        if meta.animation_type == "LOCOMOTION":
            row = layout.row(align=True)
            row.operator("echo.add_animation_marker", text="Left Contact").kind = "LEFT_FOOT"
            row.operator("echo.add_animation_marker", text="Right Contact").kind = "RIGHT_FOOT"
        for index, item in enumerate(meta.markers):
            box = layout.box()
            box.label(text=f"{item.kind.replace('_', ' ').title()}: {item.name} @ {item.frame}")
            if item.payload:
                box.label(text=f"Payload: {item.payload}")
            row = box.row(align=True)
            for operation, label, icon in (("SET", "Set Here", "MARKER_HLT"), ("JUMP", "Jump", "TIME"), ("DELETE", "Delete", "X")):
                button = row.operator("echo.animation_marker_action", text=label, icon=icon)
                button.operation, button.index = operation, index


class ECHO_PT_animation_preview(EchoPanelBase, bpy.types.Panel):
    bl_label = "Preview"
    bl_parent_id = "ECHO_PT_animation_root"
    bl_options = {"DEFAULT_CLOSED"}

    @classmethod
    def poll(cls, context):
        action = active_action(context)
        return action is not None and action.echo_animation.enabled

    def draw(self, context):
        layout, meta = self.layout, active_action(context).echo_animation
        row = layout.row(align=True)
        row.operator("echo.preview_animation", text="Full Action").mode = "FULL"
        row.operator("echo.stop_animation_preview", text="Stop", icon="PAUSE")
        if meta.animation_type in {"ONE_SHOT", "ONE_SHOT_HOLD"}:
            layout.operator("echo.preview_animation", text="Preview One Shot", icon="PLAY").mode = "ONE_SHOT"
        else:
            row = layout.row(align=True)
            row.operator("echo.preview_animation", text="Enter").mode = "ENTER"
            row.operator("echo.preview_animation", text="Loop").mode = "LOOP"
            row.operator("echo.preview_animation", text="Exit").mode = "EXIT"
            row = layout.row(align=True)
            row.operator("echo.preview_animation", text="Enter → Loop").mode = "ENTER_LOOP"
            row.operator("echo.preview_animation", text="Loop → Exit").mode = "LOOP_EXIT"


class ECHO_PT_animation_validation(EchoPanelBase, bpy.types.Panel):
    bl_label = "Validation"
    bl_parent_id = "ECHO_PT_animation_root"

    @classmethod
    def poll(cls, context):
        action = active_action(context)
        return action is not None and action.echo_animation.enabled

    def draw(self, context):
        layout, meta = self.layout, active_action(context).echo_animation
        layout.operator("echo.validate_animation", icon="CHECKMARK")
        layout.label(text=meta.validation_summary)
        for line in meta.validation_details.splitlines()[:8]:
            layout.label(text=line[:110], icon="ERROR" if "[ERROR]" in line else "INFO")
        if "Loop seam" in meta.validation_details:
            layout.operator("echo.select_problem_bones", icon="BONE_DATA")
        row = layout.row(align=True)
        for fix, label in (("RANGE", "Range from Keys"), ("CLAMP", "Clamp Markers"), ("NAMES", "Normalize Names"), ("DUPLICATES", "Remove Duplicates")):
            button = row.operator("echo.animation_fix", text=label)
            button.fix = fix
        row = layout.row(align=True)
        row.prop(meta, "position_tolerance")
        row.prop(meta, "rotation_tolerance")
        row.prop(meta, "scale_tolerance")


class ECHO_PT_animation_export(EchoPanelBase, bpy.types.Panel):
    bl_label = "Export"
    bl_parent_id = "ECHO_PT_animation_root"

    @classmethod
    def poll(cls, context):
        action = active_action(context)
        return action is not None and action.echo_animation.enabled

    def draw(self, context):
        layout, action = self.layout, active_action(context)
        meta = action.echo_animation
        layout.prop(meta, "output_directory")
        layout.prop(meta, "resource_folder")
        layout.prop(meta, "overwrite_policy")
        layout.prop(meta, "export_animation_resource")
        row = layout.row(align=True)
        row.label(text=f"Resource: {meta.resource_id}")
        copy_id = row.operator("echo.copy_text", text="", icon="COPYDOWN")
        copy_id.text = meta.resource_id
        row = layout.row(align=True)
        row.operator("echo.generate_animation_preview", icon="TEXT")
        row.operator("echo.copy_generated_es", icon="COPYDOWN")
        cached = PREVIEW_CACHE.get(action.name)
        if cached:
            for value in cached["segments"][:4]:
                layout.label(text=f"Segment: {value}")
            layout.label(text=cached["clip_path"][-100:], icon="FILE")
            layout.label(text=cached["animation_path"][-100:], icon="FILE")
        row = layout.row(align=True)
        row.operator("echo.export_action_animation", icon="EXPORT")
        row.operator("echo.export_all_animations", icon="EXPORT")
        layout.operator("echo.open_animation_output", icon="FILE_FOLDER")
        if LAST_BATCH["exported"] or LAST_BATCH["failed"]:
            layout.label(text=f"Batch: {LAST_BATCH['exported']} exported, {LAST_BATCH['warnings']} warnings, {len(LAST_BATCH['failed'])} failed")


class ECHO_PT_animation_migration(EchoPanelBase, bpy.types.Panel):
    bl_label = "Legacy Migration"
    bl_parent_id = "ECHO_PT_animation_root"
    bl_options = {"DEFAULT_CLOSED"}

    def draw(self, context):
        layout, settings = self.layout, context.window_manager.echo_animation_migration
        layout.prop(settings, "enter_action")
        layout.prop(settings, "loop_action")
        layout.prop(settings, "exit_action")
        layout.prop(settings, "new_name")
        row = layout.row(align=True)
        row.prop(settings, "source_state")
        row.prop(settings, "active_state")
        row.prop(settings, "target_state")
        row = layout.row(align=True)
        row.operator("echo.preview_legacy_migration")
        row.operator("echo.merge_legacy_actions")
        layout.label(text=settings.preview_summary[:120])


class ECHO_PT_viewport_compact(bpy.types.Panel):
    bl_label = "Echo Animation"
    bl_idname = "ECHO_PT_viewport_compact"
    bl_space_type = "VIEW_3D"
    bl_region_type = "UI"
    bl_category = "Echo"

    def draw(self, context):
        layout, action = self.layout, active_action(context)
        layout.label(text=f"Action: {action.name if action else 'None'}", icon="ACTION")
        if action and action.echo_animation.enabled:
            layout.label(text=action.echo_animation.validation_summary)
            row = layout.row(align=True)
            row.operator("echo.validate_animation", text="Validate")
            row.operator("echo.export_action_animation", text="Export")
        else:
            layout.operator("echo.create_animation", icon="ADD")


PANEL_CLASSES = (
    ECHO_PT_animation_root, ECHO_PT_animation_settings, ECHO_PT_animation_timeline,
    ECHO_PT_animation_segments, ECHO_PT_animation_events, ECHO_PT_animation_preview,
    ECHO_PT_animation_validation, ECHO_PT_animation_export, ECHO_PT_animation_migration,
    ECHO_PT_viewport_compact,
)
