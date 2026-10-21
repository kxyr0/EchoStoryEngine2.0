from __future__ import annotations

from pathlib import Path

import bpy
from bpy.props import BoolProperty

from .context import active_action, active_armature
from .export_service import export_action, generated_preview, output_paths


PREVIEW_CACHE = {}
LAST_BATCH = {"exported": 0, "warnings": 0, "failed": ()}


def _store_text(name: str, content: str):
    text = bpy.data.texts.get(name) or bpy.data.texts.new(name)
    text.clear()
    text.write(content)
    return text


class ECHO_OT_generate_export_preview(bpy.types.Operator):
    bl_idname = "echo.generate_animation_preview"
    bl_label = "Generate Output Preview"
    bl_description = "Build both generated .es resources in memory without writing files"

    def execute(self, context):
        try:
            armature, action = active_armature(context), active_action(context)
            if armature is None or action is None:
                raise RuntimeError("Select an armature with an active Action.")
            model, clip, animation, clip_path, animation_path, warnings = generated_preview(context, armature, action)
            _store_text("Echo Generated Clip.es", clip)
            _store_text("Echo Generated Animation.es", animation)
            PREVIEW_CACHE[action.name] = {
                "clip": clip, "animation": animation, "clip_path": str(clip_path), "animation_path": str(animation_path),
                "segments": tuple(f"{item.name}: {item.start_tick}–{item.end_tick}" for item in model.segments),
                "points": tuple(f"{item.name}: {item.tick}" for item in model.points),
                "events": tuple(f"{item.name}: {item.tick}" for item in model.events),
                "warnings": len(warnings),
            }
            self.report({"INFO"}, f"Preview generated: {len(model.tracks)} tracks, {model.duration_ticks} ticks.")
            return {"FINISHED"}
        except Exception as error:
            self.report({"ERROR"}, str(error))
            return {"CANCELLED"}


class ECHO_OT_copy_generated_es(bpy.types.Operator):
    bl_idname = "echo.copy_generated_es"
    bl_label = "Copy Generated .es"

    def execute(self, context):
        action = active_action(context)
        preview = PREVIEW_CACHE.get(action.name if action else "")
        if preview is None:
            self.report({"ERROR"}, "Generate the output preview first.")
            return {"CANCELLED"}
        context.window_manager.clipboard = preview["clip"] + "\n" + preview["animation"]
        self.report({"INFO"}, "Copied generated clip and animation resources.")
        return {"FINISHED"}


class ECHO_OT_export_animation(bpy.types.Operator):
    # Keep the legacy exporter on echo.export_animation.  The Action Editor
    # exporter uses its own ID because it consumes EchoAnimationMetadata and
    # serializes marker events (including item attachment markers).
    bl_idname = "echo.export_action_animation"
    bl_label = "Export Active Animation"
    bl_description = "Validate and atomically export one Action as one clip plus its generated animation resource"
    bl_options = {"REGISTER"}
    confirmed: BoolProperty(default=False, options={"HIDDEN"})

    @classmethod
    def poll(cls, context):
        action = active_action(context)
        return active_armature(context) is not None and action is not None and action.echo_animation.enabled

    def invoke(self, context, event):
        action = active_action(context)
        if action.echo_animation.overwrite_policy == "ASK" and any(path.exists() for path in output_paths(action.echo_animation, context)):
            self.confirmed = True
            return context.window_manager.invoke_confirm(self, event)
        return self.execute(context)

    def execute(self, context):
        try:
            result = export_action(context, active_armature(context), active_action(context), self.confirmed)
            files = 2 if result.animation_path else 1
            self.report({"INFO"}, f"Exported {files} files with {len(result.warnings)} warnings.")
            return {"FINISHED"}
        except Exception as error:
            self.report({"ERROR"}, str(error))
            return {"CANCELLED"}


class ECHO_OT_export_all_animations(bpy.types.Operator):
    bl_idname = "echo.export_all_animations"
    bl_label = "Export All Echo Animations"
    bl_description = "Validate every enabled Action, export valid ones, and report every skipped Action"
    bl_options = {"REGISTER"}
    confirmed: BoolProperty(default=False, options={"HIDDEN"})

    @classmethod
    def poll(cls, context):
        return active_armature(context) is not None

    def invoke(self, context, event):
        if any(action.echo_animation.enabled and action.echo_animation.overwrite_policy == "ASK" and any(path.exists() for path in output_paths(action.echo_animation, context)) for action in bpy.data.actions):
            self.confirmed = True
            return context.window_manager.invoke_confirm(self, event)
        return self.execute(context)

    def execute(self, context):
        armature = active_armature(context)
        data = armature.animation_data_create()
        original_action = data.action
        exported, warnings, failed = 0, 0, []
        try:
            for action in bpy.data.actions:
                if not action.echo_animation.enabled:
                    continue
                try:
                    data.action = action
                    result = export_action(context, armature, action, self.confirmed)
                    exported += 1
                    warnings += len(result.warnings)
                except Exception as error:
                    failed.append((action.name, action.echo_animation.resource_id, str(error)))
        finally:
            data.action = original_action
        LAST_BATCH.update(exported=exported, warnings=warnings, failed=tuple(failed))
        for action_name, resource, reason in failed:
            print(f"[Echo batch] FAILED {action_name} ({resource}): {reason}")
        message = f"Exported: {exported} | Warnings: {warnings} | Failed: {len(failed)}"
        self.report({"WARNING"} if failed else {"INFO"}, message)
        return {"FINISHED"}


class ECHO_OT_open_output_folder(bpy.types.Operator):
    bl_idname = "echo.open_animation_output"
    bl_label = "Open Output Folder"

    def execute(self, context):
        action = active_action(context)
        if action is None:
            return {"CANCELLED"}
        folder = output_paths(action.echo_animation, context)[0].parent
        folder.mkdir(parents=True, exist_ok=True)
        bpy.ops.wm.path_open(filepath=str(folder))
        return {"FINISHED"}


EXPORT_OPERATOR_CLASSES = (
    ECHO_OT_generate_export_preview, ECHO_OT_copy_generated_es, ECHO_OT_export_animation,
    ECHO_OT_export_all_animations, ECHO_OT_open_output_folder,
)
