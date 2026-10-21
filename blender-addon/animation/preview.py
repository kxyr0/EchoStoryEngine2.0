from __future__ import annotations

import bpy
from bpy.props import EnumProperty

from .context import active_action, active_armature
from .markers import HOLD, STATE_END, STATE_START, marker_frame
from .timeline import frame_range


ACTIVE_PREVIEW = {"operator": None, "cancel": False, "request_exit": False}


def capture_scene_state(scene):
    return (scene.frame_current, scene.frame_start, scene.frame_end, scene.use_preview_range,
            scene.frame_preview_start, scene.frame_preview_end, scene.sync_mode)


def restore_scene_state(scene, snapshot):
    current, start, end, use_preview, preview_start, preview_end, sync = snapshot
    scene.frame_start, scene.frame_end = start, end
    scene.use_preview_range = use_preview
    scene.frame_preview_start, scene.frame_preview_end = preview_start, preview_end
    scene.sync_mode = sync
    scene.frame_set(current)


def _ranges(action, metadata):
    start, end = frame_range(action, metadata)
    state_start = marker_frame(action, STATE_START, start)
    state_end = marker_frame(action, STATE_END, end)
    hold = marker_frame(action, HOLD, state_start)
    return {"FULL": (start, end), "ONE_SHOT": (start, end), "ENTER": (start, state_start), "LOOP": (state_start, state_end), "EXIT": (state_end, end), "HOLD": (hold, hold)}


class ECHO_OT_preview_animation(bpy.types.Operator):
    bl_idname = "echo.preview_animation"
    bl_label = "Preview Echo Animation"
    bl_description = "Preview runtime-style phases without permanently changing Scene playback settings"
    bl_options = {"INTERNAL"}

    mode: EnumProperty(items=(("FULL", "Full Action", "Play full Action"), ("ENTER", "Enter", "Play Enter once"), ("LOOP", "Loop", "Repeat Loop"), ("EXIT", "Exit", "Play Exit once"), ("ONE_SHOT", "One Shot", "Play once"), ("ENTER_LOOP", "Enter to Loop", "Play Enter, then repeat Loop"), ("LOOP_EXIT", "Loop to Exit", "Loop until Stop Preview, then play Exit"), ("HOLD", "Hold", "Show the held pose")))
    _timer = None
    _snapshot = None
    _ranges = None
    _phase = ""
    _frame = 0
    _action_name = ""

    @classmethod
    def poll(cls, context):
        action = active_action(context)
        return active_armature(context) is not None and action is not None and action.echo_animation.enabled

    def execute(self, context):
        if ACTIVE_PREVIEW["operator"] is not None:
            self.report({"WARNING"}, "An Echo preview is already running.")
            return {"CANCELLED"}
        action = active_action(context)
        metadata = action.echo_animation
        self._ranges = _ranges(action, metadata)
        self._phase = "ENTER" if self.mode == "ENTER_LOOP" else "LOOP" if self.mode == "LOOP_EXIT" else self.mode
        self._frame = self._ranges[self._phase][0]
        self._action_name = action.name
        scene = context.scene
        self._snapshot = capture_scene_state(scene)
        scene.frame_set(self._frame)
        fps = max(1.0, scene.render.fps / scene.render.fps_base)
        self._timer = context.window_manager.event_timer_add(1.0 / fps, window=context.window)
        context.window_manager.modal_handler_add(self)
        ACTIVE_PREVIEW.update(operator=self, cancel=False, request_exit=False)
        return {"RUNNING_MODAL"}

    def modal(self, context, event):
        if event.type == "ESC" or ACTIVE_PREVIEW["cancel"]:
            self._restore(context)
            return {"CANCELLED"}
        if event.type != "TIMER":
            return {"PASS_THROUGH"}
        try:
            if active_action(context) is None or active_action(context).name != self._action_name:
                raise RuntimeError("Active Action changed during preview.")
            first, last = self._ranges[self._phase]
            if self._frame < last:
                self._frame += 1
            elif self._phase == "ENTER" and self.mode == "ENTER_LOOP":
                self._phase, self._frame = "LOOP", self._ranges["LOOP"][0]
            elif self._phase == "LOOP" and self.mode == "LOOP_EXIT" and ACTIVE_PREVIEW["request_exit"]:
                self._phase, self._frame = "EXIT", self._ranges["EXIT"][0]
            elif self._phase == "LOOP":
                self._frame = first
            elif self.mode == "ONE_SHOT" and active_action(context).echo_animation.hold_last_frame:
                self._frame = last
            elif self.mode == "HOLD":
                self._frame = last
            else:
                self._restore(context)
                return {"FINISHED"}
            context.scene.frame_set(self._frame)
            context.view_layer.update()
            return {"RUNNING_MODAL"}
        except Exception as error:
            self._restore(context)
            self.report({"ERROR"}, str(error))
            return {"CANCELLED"}

    def _restore(self, context):
        if self._timer is not None:
            context.window_manager.event_timer_remove(self._timer)
            self._timer = None
        scene = context.scene
        restore_scene_state(scene, self._snapshot)
        context.view_layer.update()
        ACTIVE_PREVIEW.update(operator=None, cancel=False, request_exit=False)


class ECHO_OT_stop_preview(bpy.types.Operator):
    bl_idname = "echo.stop_animation_preview"
    bl_label = "Stop Preview"
    bl_description = "Stop preview; Loop to Exit waits for the shared loop boundary and then plays Exit"

    def execute(self, _context):
        operator = ACTIVE_PREVIEW["operator"]
        if operator is None:
            return {"CANCELLED"}
        if operator.mode == "LOOP_EXIT":
            ACTIVE_PREVIEW["request_exit"] = True
        else:
            ACTIVE_PREVIEW["cancel"] = True
        return {"FINISHED"}


PREVIEW_CLASSES = (ECHO_OT_preview_animation, ECHO_OT_stop_preview)
