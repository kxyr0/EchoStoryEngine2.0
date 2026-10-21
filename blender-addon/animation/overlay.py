from __future__ import annotations

import bpy

from .context import active_action, preferences
from .markers import HOLD, STATE_END, STATE_START, marker_frame, sync_from_action
from .timeline import frame_range


_HANDLE = None


def _bands(action, metadata):
    start, end = frame_range(action, metadata)
    state_start = marker_frame(action, STATE_START, start)
    state_end = marker_frame(action, STATE_END, end)
    if metadata.animation_type in {"ONE_SHOT", "ONE_SHOT_HOLD"}:
        return (("ONE SHOT", start, end, (0.22, 0.48, 0.85, 0.18)),)
    if metadata.animation_type == "STATEFUL_HOLD":
        hold = marker_frame(action, HOLD, state_start)
        result = []
        if metadata.enter_enabled and start < hold:
            result.append(("ENTER", start, hold, (0.24, 0.65, 0.42, 0.18)))
        result.append(("HOLD", hold - 0.3, hold + 0.3, (0.92, 0.68, 0.18, 0.35)))
        if metadata.exit_enabled and hold < end:
            result.append(("EXIT", hold, end, (0.78, 0.35, 0.40, 0.18)))
        return tuple(result)
    result = []
    if metadata.enter_enabled and start < state_start:
        result.append(("ENTER", start, state_start, (0.24, 0.65, 0.42, 0.18)))
    if metadata.loop_enabled:
        result.append(("LOOP", state_start, state_end, (0.25, 0.48, 0.88, 0.18)))
    if metadata.exit_enabled and state_end < end:
        result.append(("EXIT", state_end, end, (0.78, 0.35, 0.40, 0.18)))
    return tuple(result)


def draw_overlay():
    context = bpy.context
    if context.area is None or context.area.type != "DOPESHEET_EDITOR":
        return
    prefs = preferences(context)
    if prefs is not None and not prefs.overlay_enabled:
        return
    action = active_action(context)
    if action is None or not action.echo_animation.enabled:
        return
    sync_from_action(action)
    import blf
    import gpu
    from gpu_extras.batch import batch_for_shader
    region, view = context.region, context.region.view2d
    shader = gpu.shader.from_builtin("UNIFORM_COLOR")
    gpu.state.blend_set("ALPHA")
    for label, start, end, color in _bands(action, action.echo_animation):
        x1 = view.view_to_region(start, 0, clip=False)[0]
        x2 = view.view_to_region(end, 0, clip=False)[0]
        if x2 < 0 or x1 > region.width:
            continue
        x1, x2 = max(0, x1), min(region.width, x2)
        vertices = ((x1, 6), (x2, 6), (x2, 27), (x1, 27))
        batch = batch_for_shader(shader, "TRI_FAN", {"pos": vertices})
        shader.bind()
        shader.uniform_float("color", color)
        batch.draw(shader)
        if x2 - x1 > 44:
            blf.position(0, x1 + 5, 11, 0)
            blf.size(0, 10)
            blf.color(0, 0.9, 0.9, 0.9, 0.75)
            blf.draw(0, label)
    gpu.state.blend_set("NONE")


def register_overlay():
    global _HANDLE
    if _HANDLE is None:
        _HANDLE = bpy.types.SpaceDopeSheetEditor.draw_handler_add(draw_overlay, (), "WINDOW", "POST_PIXEL")


def unregister_overlay():
    global _HANDLE
    if _HANDLE is not None:
        bpy.types.SpaceDopeSheetEditor.draw_handler_remove(_HANDLE, "WINDOW")
        _HANDLE = None
