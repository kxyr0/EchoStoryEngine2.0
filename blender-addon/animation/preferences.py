from __future__ import annotations

import bpy
from bpy.props import BoolProperty, EnumProperty, StringProperty


ADDON_PACKAGE = (__package__ or __name__).split(".", 1)[0]
DEFAULT_OUTPUT_ROOT = (
    r"C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances"
    r"\Форма Мысли\minecraft\story\content-pack\jdh\echo_animation"
)


def _update_shortcuts(self, _context):
    from .keymaps import register_shortcuts
    register_shortcuts(self)


class EchoAnimationPreferences(bpy.types.AddonPreferences):
    bl_idname = ADDON_PACKAGE

    output_root: StringProperty(
        name="Default Output Directory",
        description="Folder containing animation_clips and animations in the live story content pack",
        subtype="DIR_PATH",
        default=DEFAULT_OUTPUT_ROOT,
    )
    resource_folder: StringProperty(
        name="Default Resource Folder",
        description="Storage folder below animations and animation_clips; runtime IDs remain flat",
        default="chapter_1/family",
    )
    overlay_enabled: BoolProperty(
        name="Timeline Range Overlay",
        description="Draw unobtrusive ENTER, LOOP, HOLD and EXIT bands in the Action Editor",
        default=True,
    )
    shortcuts_enabled: BoolProperty(
        name="Enable Echo Marker Shortcuts",
        description="Register optional non-default marker shortcuts in the Action Editor",
        default=False,
        update=_update_shortcuts,
    )
    shortcut_modifier: EnumProperty(
        name="Shortcut Modifier",
        items=(("ALT_SHIFT", "Alt + Shift", "Use Alt and Shift"), ("CTRL_SHIFT", "Ctrl + Shift", "Use Ctrl and Shift")),
        default="ALT_SHIFT",
        update=_update_shortcuts,
    )

    def draw(self, _context):
        layout = self.layout
        layout.prop(self, "output_root")
        layout.prop(self, "resource_folder")
        layout.prop(self, "overlay_enabled")
        layout.prop(self, "shortcuts_enabled")
        if self.shortcuts_enabled:
            layout.prop(self, "shortcut_modifier")
            layout.label(text="L = Loop Start, E = Loop End, H = Hold Pose", icon="INFO")


PREFERENCE_CLASSES = (EchoAnimationPreferences,)
