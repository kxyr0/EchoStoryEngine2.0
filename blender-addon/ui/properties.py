from __future__ import annotations

import bpy
from bpy.props import BoolProperty, EnumProperty, StringProperty

from ..rig.schema import AUTO_SCHEMA_ID, LEGACY_RIG_ID, LEGACY_SCHEMA_ID, LEGACY_V1_SCHEMA_ID
from ..rig.therig import THERIG_SCHEMA_ID


PANEL_STATE_DEFAULTS = {
    "echo_ui_show_rig_block": True,
    "echo_ui_show_animation_block": True,
    "echo_ui_show_motion_presets": True,
    "echo_ui_show_skin_block": False,
    "echo_ui_show_ik_block": False,
    "echo_ui_show_ik_help": False,
}


PANEL_MODE_ITEMS = (
    ("ANIMATOR", "Аниматор", "Показывать в первую очередь инструменты анимации"),
    ("MODELER", "Модельер", "Показывать в первую очередь экспорт модели и скининг"),
    ("ALL", "Всё", "Показывать все секции"),
)


def register_properties() -> None:
    if not hasattr(bpy.types.Armature, "echo_rig_id"):
        bpy.types.Armature.echo_rig_id = StringProperty(
            name="ID скелета Echo",
            description="Игровой ID скелета. По нему формируется путь к файлу .skin.json. Обычно менять это поле не нужно.",
            default=LEGACY_RIG_ID,
        )
    if not hasattr(bpy.types.Armature, "echo_rig_schema"):
        bpy.types.Armature.echo_rig_schema = EnumProperty(
            name="Схема скелета",
            description="Набор и порядок костей, который используют экспорт модели и анимаций. Не меняйте без причины.",
            items=(
                (
                    LEGACY_SCHEMA_ID,
                    "JDH Humanoid v2 — корпус 2 кости",
                    "Новый стандарт: body_lower + body (верх корпуса), всего 14 runtime-костей.",
                ),
                (
                    LEGACY_V1_SCHEMA_ID,
                    "JDH Humanoid v1 — старый",
                    "Совместимость со старыми 13-костными ригами. Для новых анимаций обновите корпус до v2.",
                ),
                (
                    THERIG_SCHEMA_ID,
                    "TheRig 2.6 (полный deform-скелет)",
                    "Экспортирует полный deform/lattice-скелет TheRig 2.6. Контролы запекаются в итоговые кости автоматически.",
                ),
                (
                    AUTO_SCHEMA_ID,
                    "Runtime / Deform иерархия",
                    "Автоматически использует deform-кости и необходимые родительские кости, исключая управляющие контролы.",
                ),
            ),
            default=LEGACY_SCHEMA_ID,
        )

    if not hasattr(bpy.types.Scene, "echo_ui_mode"):
        bpy.types.Scene.echo_ui_mode = EnumProperty(
            name="Режим панели",
            description="Какой набор инструментов показывать в боковой панели",
            items=PANEL_MODE_ITEMS,
            default="ANIMATOR",
        )

    if not hasattr(bpy.types.Scene, "echo_last_export_json"):
        bpy.types.Scene.echo_last_export_json = StringProperty(
            name="Последний экспорт JSON",
            default="",
            options={'HIDDEN'},
        )
    if not hasattr(bpy.types.Scene, "echo_last_export_label"):
        bpy.types.Scene.echo_last_export_label = StringProperty(
            name="Последний экспорт",
            default="",
        )
    if not hasattr(bpy.types.Scene, "echo_last_export_path"):
        bpy.types.Scene.echo_last_export_path = StringProperty(
            name="Путь последнего экспорта",
            default="",
            subtype='FILE_PATH',
        )

    panel_states = {
        "echo_ui_show_rig_block": (
            "Показывать блок скелета",
            "Развернуть блок с данными скелета и схемы",
        ),
        "echo_ui_show_animation_block": (
            "Показывать блок экспорта анимации",
            "Развернуть блок экспорта анимации",
        ),
        "echo_ui_show_motion_presets": (
            "Показывать Modern пресеты",
            "Развернуть генераторы Modern Idle и Modern Walk",
        ),
        "echo_ui_show_skin_block": (
            "Показывать блок скининга",
            "Развернуть блок экспорта модели и проверки скининга",
        ),
        "echo_ui_show_ik_block": (
            "Показывать блок IK",
            "Развернуть блок инструментов IK",
        ),
        "echo_ui_show_ik_help": (
            "Показывать подсказки IK",
            "Развернуть текстовые подсказки по работе с IK",
        ),
    }
    for prop_name, (name, description) in panel_states.items():
        if not hasattr(bpy.types.Scene, prop_name):
            setattr(
                bpy.types.Scene,
                prop_name,
                BoolProperty(
                    name=name,
                    description=description,
                    default=PANEL_STATE_DEFAULTS[prop_name],
                ),
            )


def unregister_properties() -> None:
    for name in (
        "echo_rig_schema",
        "echo_rig_id",
    ):
        if hasattr(bpy.types.Armature, name):
            delattr(bpy.types.Armature, name)

    for name in (
        "echo_ui_mode",
        "echo_last_export_json",
        "echo_last_export_label",
        "echo_last_export_path",
        "echo_ui_show_rig_block",
        "echo_ui_show_animation_block",
        "echo_ui_show_motion_presets",
        "echo_ui_show_skin_block",
        "echo_ui_show_ik_block",
        "echo_ui_show_ik_help",
    ):
        if hasattr(bpy.types.Scene, name):
            delattr(bpy.types.Scene, name)
