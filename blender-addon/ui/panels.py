from __future__ import annotations

import bpy

from .. import ik_tools
from ..motion_presets import ECHO_OT_create_modern_idle, ECHO_OT_create_modern_walk
from ..animation.importer import ECHO_OT_import_animation_es
from ..animation.exporter import (
    ECHO_OT_copy_text,
    ECHO_OT_export_animation,
    ECHO_OT_open_addon_preferences,
    ECHO_OT_open_export_root,
    ECHO_OT_open_last_export_location,
    ECHO_OT_quick_export_animation,
    ECHO_OT_repeat_last_export,
    behavior_title,
    get_active_action,
    get_active_armature,
    guess_animation_behavior,
    runtime_animation_id,
    sanitize_animation_name,
)
from ..operators.assign_rigid_attachment import ECHO_OT_assign_rigid_attachment
from ..operators.export_skin import ECHO_OT_export_skin
from ..operators.validate_skin import ECHO_OT_validate_skin
from ..operators.upgrade_body_rig import ECHO_OT_upgrade_split_body_rig, has_split_body
from ..rig.schema import resolve_skeleton_schema
from ..rig.therig import THERIG_SCHEMA_ID
from ..skin.exporter import armature_for_mesh


def _section_header(container, data, prop_name: str, title: str, icon: str):
    row = container.row(align=True)
    expanded = bool(getattr(data, prop_name))
    row.prop(
        data,
        prop_name,
        text=title,
        icon="TRIA_DOWN" if expanded else "TRIA_RIGHT",
        emboss=False,
    )
    row.label(text="", icon=icon)
    return expanded


class ECHO_PT_export_panel(bpy.types.Panel):
    bl_label = "Echo — анимация и риг"
    bl_idname = "ECHO_PT_export_panel"
    bl_space_type = "VIEW_3D"
    bl_region_type = "UI"
    bl_category = "Echo"

    def draw(self, context):
        layout = self.layout
        scene = context.scene
        armature = get_active_armature(context)
        mesh = context.object if context.object is not None and context.object.type == "MESH" else None
        if armature is None and mesh is not None:
            try:
                armature = armature_for_mesh(mesh)
            except RuntimeError:
                pass

        layout.label(text="Echo Animation 3.3.1 RU — System Sit Pose", icon="CHECKMARK")
        mode_box = layout.box()
        mode_box.label(text="Режим интерфейса", icon="WINDOW")
        mode_box.prop(scene, "echo_ui_mode", expand=True)
        row = mode_box.row(align=True)
        row.operator(ECHO_OT_open_addon_preferences.bl_idname, text="Настройки", icon="PREFERENCES")
        row.operator(ECHO_OT_open_export_root.bl_idname, text="Папка экспорта", icon="FILE_FOLDER")

        if scene.echo_last_export_label:
            last_box = layout.box()
            last_box.label(text="Последний экспорт", icon="RECOVER_LAST")
            last_box.label(text=scene.echo_last_export_label)
            row = last_box.row(align=True)
            row.operator(ECHO_OT_repeat_last_export.bl_idname, text="Повторить", icon="FILE_REFRESH")
            row.operator(ECHO_OT_open_last_export_location.bl_idname, text="Папка", icon="FILE_FOLDER")

        if armature is None:
            layout.label(text="Выберите скелет Echo или меш со скинингом", icon="ARMATURE_DATA")
            help_box = layout.box()
            help_box.label(text="Для анимации выберите Armature.", icon="INFO")
            help_box.label(text="Потом выберите Action и ставьте ключи.")
            return

        rig_container = layout.box()
        if _section_header(rig_container, scene, "echo_ui_show_rig_block", "Скелет и схема", "ARMATURE_DATA"):
            resolved = None
            try:
                resolved = resolve_skeleton_schema(armature)
                rig_id = resolved.rig_resource_id
                rig_container.label(text=f"Скелет: {armature.name}", icon="ARMATURE_DATA")
                rig_container.label(text=f"Определено: {resolved.schema_id} | костей: {len(resolved.bones)}")
            except RuntimeError as error:
                rig_id = armature.data.echo_rig_id
                rig_container.label(text=f"Скелет: {armature.name}", icon="ARMATURE_DATA")
                rig_container.label(text=str(error), icon="ERROR")
            automatic_therig = (
                resolved is not None
                and resolved.schema_id == THERIG_SCHEMA_ID
                and armature.data.get("echo_rig_id") is None
                and armature.data.get("echo_rig_schema") is None
            )
            if automatic_therig:
                rig_container.label(text=f"ID скелета (авто): {rig_id}", icon="CHECKMARK")
                rig_container.label(text="Схема (авто): TheRig 2.6")
            else:
                rig_container.prop(armature.data, "echo_rig_id")
                rig_container.prop(armature.data, "echo_rig_schema")

            if has_split_body(armature):
                if resolved is None or resolved.schema_id != THERIG_SCHEMA_ID:
                    rig_container.label(text="Корпус: body_lower + body (верх)", icon="CHECKMARK")
            elif armature.data.bones.get("body") is not None:
                warning = rig_container.box()
                warning.label(text="Старый корпус: одна кость body", icon="ERROR")
                warning.operator(
                    ECHO_OT_upgrade_split_body_rig.bl_idname,
                    text="Разделить корпус: body_lower + body",
                    icon="BONE_DATA",
                )
                warning.label(text="Старые body-ключи останутся на нижней кости.", icon="INFO")
            namespace, separator, path = rig_id.partition(":")
            if separator and namespace and path:
                rig_container.label(
                    text=f"assets/{namespace}/echo_animation/models/{path}.skin.json",
                    icon="FILE",
                )

        if scene.echo_ui_mode in {"ANIMATOR", "ALL"}:
            motion_container = layout.box()
            if _section_header(motion_container, scene, "echo_ui_show_motion_presets", "Modern анимации", "ARMATURE_DATA"):
                motion_container.label(text="Готовые циклы без ванильного маятника.", icon="INFO")
                row = motion_container.row(align=True)
                row.operator(ECHO_OT_create_modern_idle.bl_idname, text="Создать Modern Idle", icon="PAUSE")
                row.operator(ECHO_OT_create_modern_walk.bl_idname, text="Создать Modern Walk", icon="PLAY")
                motion_container.label(text="Двигаются root, корпус, голова, плечи, руки, локти, ноги и колени.")
                motion_container.label(text="На TheRig дополнительно используются кисти и стопы; экспорт сразу настроен как Loop.")

        if scene.echo_ui_mode in {"ANIMATOR", "ALL"}:
            animation_container = layout.box()
            if _section_header(animation_container, scene, "echo_ui_show_animation_block", "Экспорт анимации", "ACTION"):
                action = get_active_action(armature)
                animation_container.label(text=f"Action: {action.name if action else 'не выбран'}")
                if action is not None:
                    animation_id = runtime_animation_id(action.name)
                    row = animation_container.row(align=True)
                    row.label(text=animation_id, icon="LINKED")
                    auto_behavior = guess_animation_behavior(action.name)
                    animation_container.label(
                        text=f"Новый Action без настроек: {behavior_title(auto_behavior)}",
                        icon="FILE_REFRESH" if auto_behavior == "LOOP" else "PLAY",
                    )
                    copy_id = row.operator(ECHO_OT_copy_text.bl_idname, text="", icon="COPYDOWN")
                    copy_id.text = animation_id
                row = animation_container.row(align=True)
                quick_text = "Экспортировать системный Sit" if action is not None and sanitize_animation_name(action.name) in {"pose_sit", "sit_pose"} else "Быстрый экспорт"
                row.operator(ECHO_OT_quick_export_animation.bl_idname, text=quick_text, icon="CHECKMARK")
                row.operator(ECHO_OT_export_animation.bl_idname, text="Настройки экспорта", icon="PREFERENCES")
                if action is not None and sanitize_animation_name(action.name) in {"pose_sit", "sit_pose"}:
                    sit_box = animation_container.box()
                    sit_box.label(text="Системная поза SIT", icon="ARMATURE_DATA")
                    sit_box.label(text="Экспортируется как настоящий Hold до Stop — без Enter/Exit и без 100000 тиков.")
                    sit_box.label(text="Runtime сам применит jdh:pose_sit_main ко всем NPC, когда context.sitting = true.")
                animation_container.operator(
                    ECHO_OT_import_animation_es.bl_idname,
                    text="Импортировать .es → Action с ключами",
                    icon="IMPORT",
                )
                animation_container.label(text="Импорт понимает .animation.es и .animation-clip.es, включая Loop / Hold / Фазы.", icon="INFO")
                animation_container.label(text="Быстрый экспорт: idle/walk/run автоматически идут как Loop; настройки Action запоминаются.")

        if scene.echo_ui_mode in {"MODELER", "ALL"}:
            skin_container = layout.box()
            if _section_header(skin_container, scene, "echo_ui_show_skin_block", "Модель со скинингом", "MESH_DATA"):
                skin_container.operator(ECHO_OT_export_skin.bl_idname, text="Экспортировать модель", icon="EXPORT")
                row = skin_container.row(align=True)
                row.operator(ECHO_OT_validate_skin.bl_idname, text="Проверить модель", icon="CHECKMARK")
                row.operator(ECHO_OT_assign_rigid_attachment.bl_idname, text="Жёсткая привязка", icon="GROUP_VERTEX")
                skin_container.label(text="Для модельера: deform/lattice кости | runtime максимум: 512")
                skin_container.label(text="Для обычной анимации этот блок не нужен.", icon="INFO")

        if scene.echo_ui_mode in {"ANIMATOR", "ALL"}:
            ik_tools.draw_ik_panel(layout, context)


PANEL_CLASSES = (ECHO_PT_export_panel,)
