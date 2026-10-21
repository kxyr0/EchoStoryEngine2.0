from __future__ import annotations

import bpy


def active_armature(context: bpy.types.Context) -> bpy.types.Object | None:
    obj = context.object
    if obj is not None and obj.type == "ARMATURE":
        return obj
    if obj is not None and obj.type == "MESH":
        rigs = [modifier.object for modifier in obj.modifiers if modifier.type == "ARMATURE" and modifier.object]
        if len(rigs) == 1:
            return rigs[0]
    selected = [item for item in context.selected_objects if item.type == "ARMATURE"]
    return selected[0] if len(selected) == 1 else None


def active_action(context: bpy.types.Context):
    armature = active_armature(context)
    data = armature.animation_data if armature else None
    return data.action if data else None


def preferences(context: bpy.types.Context):
    package = (__package__ or __name__).split(".", 1)[0]
    addon = context.preferences.addons.get(package)
    return addon.preferences if addon else None
