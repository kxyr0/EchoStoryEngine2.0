from __future__ import annotations

import bpy

from ..animation.exporter import get_active_armature
from ..rig.schema import LEGACY_SCHEMA_ID


def has_split_body(armature) -> bool:
    return armature is not None and armature.data.bones.get("body_lower") is not None and armature.data.bones.get("body") is not None


class ECHO_OT_upgrade_split_body_rig(bpy.types.Operator):
    bl_idname = "echo.upgrade_split_body_rig"
    bl_label = "Разделить корпус на 2 кости"
    bl_description = (
        "Обновить старый JDH Humanoid: существующий body становится body_lower, "
        "а верхняя половина создаётся как новый body. Старые ключи body переносятся на body_lower."
    )
    bl_options = {"REGISTER", "UNDO"}

    @classmethod
    def poll(cls, context):
        armature = get_active_armature(context)
        return armature is not None and armature.data.bones.get("body") is not None

    def execute(self, context):
        armature = get_active_armature(context)
        if armature is None:
            self.report({"ERROR"}, "Выберите Armature")
            return {"CANCELLED"}

        if has_split_body(armature):
            armature.data.echo_rig_schema = LEGACY_SCHEMA_ID
            self.report({"INFO"}, "Корпус уже разделён: body_lower + body")
            return {"FINISHED"}

        body_data = armature.data.bones.get("body")
        if body_data is None:
            self.report({"ERROR"}, "Кость body не найдена")
            return {"CANCELLED"}

        old_mode = armature.mode
        old_active = context.view_layer.objects.active
        try:
            context.view_layer.objects.active = armature
            armature.select_set(True)
            if armature.mode != "EDIT":
                bpy.ops.object.mode_set(mode="EDIT")

            edit_bones = armature.data.edit_bones
            old_body = edit_bones.get("body")
            if old_body is None:
                raise RuntimeError("Не удалось получить edit bone 'body'")

            original_head = old_body.head.copy()
            original_tail = old_body.tail.copy()
            if (original_tail - original_head).length < 1e-5:
                raise RuntimeError("Кость body имеет нулевую длину и не может быть разделена")
            midpoint = (original_head + original_tail) * 0.5
            original_parent = old_body.parent
            original_connect = old_body.use_connect
            original_deform = old_body.use_deform
            children = list(old_body.children)

            # Rename first so existing Blender animation paths keep the old whole-torso motion.
            old_body.name = "body_lower"
            lower = old_body
            lower.head = original_head
            lower.tail = midpoint
            lower.parent = original_parent
            lower.use_connect = original_connect

            upper = edit_bones.new("body")
            upper.head = midpoint
            upper.tail = original_tail
            upper.parent = lower
            upper.use_connect = True
            upper.use_deform = original_deform

            # Head and shoulders belong to the upper chest. Other unusual children stay on lower.
            for child in children:
                if child.name in {"head", "left_shoulder", "right_shoulder"}:
                    child.parent = upper
                    child.use_connect = False

            bpy.ops.object.mode_set(mode="POSE")

            # Blender normally rewrites FCurve paths on bone rename; enforce it for old files/addons.
            old_path = 'pose.bones["body"]'
            lower_path = 'pose.bones["body_lower"]'
            for action in bpy.data.actions:
                for curve in action.fcurves:
                    if old_path in curve.data_path:
                        curve.data_path = curve.data_path.replace(old_path, lower_path)

            lower_data = armature.data.bones.get("body_lower")
            upper_data = armature.data.bones.get("body")
            if lower_data is not None:
                lower_data["echo_runtime_name"] = "body_lower"
            if upper_data is not None:
                upper_data["echo_runtime_name"] = "body"
            armature.data.echo_rig_schema = LEGACY_SCHEMA_ID

            self.report({"INFO"}, "Готово: body_lower (низ) + body (верх). Старые body-ключи сохранены на body_lower.")
            return {"FINISHED"}
        except Exception as error:
            self.report({"ERROR"}, str(error))
            return {"CANCELLED"}
        finally:
            try:
                target_mode = "OBJECT" if old_mode == "OBJECT" else old_mode
                if armature.mode != target_mode:
                    bpy.ops.object.mode_set(mode=target_mode)
            except Exception:
                pass
            if old_active is not None:
                context.view_layer.objects.active = old_active


UPGRADE_BODY_RIG_CLASSES = (ECHO_OT_upgrade_split_body_rig,)
