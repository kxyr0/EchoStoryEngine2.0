from __future__ import annotations

import bpy


_KEYMAPS = []


def register_shortcuts(preferences=None):
    unregister_shortcuts()
    if preferences is None or not preferences.shortcuts_enabled:
        return
    keyconfig = bpy.context.window_manager.keyconfigs.addon
    if keyconfig is None:
        return
    keymap = keyconfig.keymaps.new(name="Dopesheet", space_type="DOPESHEET_EDITOR")
    ctrl = preferences.shortcut_modifier == "CTRL_SHIFT"
    alt = preferences.shortcut_modifier == "ALT_SHIFT"
    for key, boundary in (("L", "START"), ("E", "END"), ("H", "HOLD")):
        item = keymap.keymap_items.new("echo.set_animation_boundary", key, "PRESS", shift=True, ctrl=ctrl, alt=alt)
        item.properties.boundary = boundary
        _KEYMAPS.append((keymap, item))


def unregister_shortcuts():
    for keymap, item in _KEYMAPS:
        try:
            keymap.keymap_items.remove(item)
        except (ReferenceError, RuntimeError):
            pass
    _KEYMAPS.clear()
