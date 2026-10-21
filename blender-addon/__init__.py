from __future__ import annotations

bl_info = {
    "name": "Echo Animation Exporter RU",
    "author": "No Echo",
    "version": (3, 3, 1),
    "blender": (4, 3, 0),
    "location": "Файл > Импорт/Экспорт; 3D Viewport > боковая панель (N) > Echo",
    "description": "Echo Import/Export + двухкостный корпус body_lower/body + Modern Motion",
    "category": "Import-Export",
}

import bpy

from . import ik_tools
from .motion_presets import MOTION_PRESET_CLASSES
from .animation.exporter import ANIMATION_CLASSES
from .animation.importer import IMPORT_CLASSES
from .animation.properties import (
    PROPERTY_CLASSES as ANIMATION_PROPERTY_CLASSES,
    register_action_properties,
    unregister_action_properties,
)
from .animation.operators import OPERATOR_CLASSES as ANIMATION_OPERATOR_CLASSES
from .animation.export_operators import EXPORT_OPERATOR_CLASSES
from .animation.preview import PREVIEW_CLASSES
from .animation.migration import MIGRATION_CLASSES
from .animation.panels import PANEL_CLASSES as ANIMATION_PANEL_CLASSES
from .operators.assign_rigid_attachment import RIGID_ATTACHMENT_CLASSES
from .operators.export_animation import draw_animation_export_menu
from .operators.import_animation import draw_animation_import_menu
from .operators.export_skin import SKIN_EXPORT_CLASSES, draw_skin_export_menu
from .operators.validate_skin import SKIN_VALIDATION_CLASSES
from .operators.upgrade_body_rig import UPGRADE_BODY_RIG_CLASSES
from .ui.panels import PANEL_CLASSES
from .ui.properties import register_properties, unregister_properties


LEGACY_CLASSES = (
    *ANIMATION_CLASSES,
    *IMPORT_CLASSES,
    *MOTION_PRESET_CLASSES,
    *ik_tools.IK_CLASSES,
    *SKIN_EXPORT_CLASSES,
    *SKIN_VALIDATION_CLASSES,
    *RIGID_ATTACHMENT_CLASSES,
    *UPGRADE_BODY_RIG_CLASSES,
    *PANEL_CLASSES,
)

# The legacy exporter already owns echo.copy_text.  The Action Editor stack
# defines a compatible helper with the same bl_idname, so registering both
# would make Blender reject the add-on.  Keep the established operator.
ACTION_OPERATOR_CLASSES = tuple(
    cls
    for cls in ANIMATION_OPERATOR_CLASSES
    if getattr(cls, "bl_idname", "") != "echo.copy_text"
)

ACTION_EDITOR_CLASSES = (
    *ACTION_OPERATOR_CLASSES,
    *EXPORT_OPERATOR_CLASSES,
    *PREVIEW_CLASSES,
    *MIGRATION_CLASSES,
    *ANIMATION_PANEL_CLASSES,
)


def register():
    for cls in LEGACY_CLASSES:
        bpy.utils.register_class(cls)

    # PointerProperty targets must be registered before the properties are
    # attached to bpy.types.Action / WindowManager.
    for cls in ANIMATION_PROPERTY_CLASSES:
        bpy.utils.register_class(cls)

    register_properties()
    register_action_properties()
    ik_tools.register_properties()

    for cls in ACTION_EDITOR_CLASSES:
        bpy.utils.register_class(cls)

    bpy.types.TOPBAR_MT_file_export.append(draw_animation_export_menu)
    bpy.types.TOPBAR_MT_file_import.append(draw_animation_import_menu)
    bpy.types.TOPBAR_MT_file_export.append(draw_skin_export_menu)


def unregister():
    try:
        bpy.types.TOPBAR_MT_file_import.remove(draw_animation_import_menu)
    except ValueError:
        pass
    for menu in (draw_skin_export_menu, draw_animation_export_menu):
        try:
            bpy.types.TOPBAR_MT_file_export.remove(menu)
        except ValueError:
            pass
    for cls in reversed(ACTION_EDITOR_CLASSES):
        try:
            bpy.utils.unregister_class(cls)
        except RuntimeError:
            pass

    ik_tools.unregister_properties()
    unregister_action_properties()
    unregister_properties()

    for cls in reversed(ANIMATION_PROPERTY_CLASSES):
        try:
            bpy.utils.unregister_class(cls)
        except RuntimeError:
            pass

    for cls in reversed(LEGACY_CLASSES):
        try:
            bpy.utils.unregister_class(cls)
        except RuntimeError:
            pass


if __name__ == "__main__":
    register()
