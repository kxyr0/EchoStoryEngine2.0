from __future__ import annotations


PRESETS = {
    "IDLE": {"label": "Idle Loop", "type": "LOOP", "channel": "BODY", "active": "idle", "blend": 4},
    "WALK": {"label": "Walk Loop", "type": "LOCOMOTION", "channel": "BODY", "active": "walk", "blend": 3},
    "RUN": {"label": "Run Loop", "type": "LOCOMOTION", "channel": "BODY", "active": "run", "blend": 3},
    "RUN_STATEFUL": {"label": "Run with Start and Stop", "type": "LOCOMOTION", "channel": "BODY", "active": "run", "enter": True, "exit": True},
    "SIT": {"label": "Sit", "type": "STATEFUL_LOOP", "channel": "BODY", "active": "sitting", "enter": True, "exit": True},
    "SLEEP": {"label": "Sleep", "type": "STATEFUL_LOOP", "channel": "BODY", "active": "sleeping", "enter": True, "exit": True},
    "LEAN": {"label": "Lean Against Wall", "type": "STATEFUL_HOLD", "channel": "BODY", "active": "leaning", "enter": True, "exit": True},
    "UPPER_GESTURE": {"label": "Upper Body Gesture", "type": "ONE_SHOT", "channel": "UPPER_BODY", "active": "gesture"},
    "LOOK": {"label": "Look Direction", "type": "ONE_SHOT", "channel": "HEAD", "active": "look"},
    "ONE_SHOT": {"label": "One Shot", "type": "ONE_SHOT", "channel": "BODY", "active": "action"},
    "DEATH": {"label": "Death", "type": "ONE_SHOT_HOLD", "channel": "BODY", "active": "dead", "hold": True},
    "HOLD_OBJECT": {"label": "Hold Object", "type": "STATEFUL_HOLD", "channel": "UPPER_BODY", "active": "holding", "enter": True, "exit": True},
}


def items():
    return tuple((key, value["label"], "Fill Echo metadata without changing bone animation") for key, value in PRESETS.items())


def apply(metadata, preset_id: str) -> None:
    preset = PRESETS[preset_id]
    metadata.animation_type = preset["type"]
    metadata.channel = preset["channel"]
    metadata.bone_mask = preset["channel"]
    metadata.active_state = preset["active"]
    metadata.enter_enabled = preset.get("enter", False)
    metadata.loop_enabled = preset["type"] in {"LOOP", "STATEFUL_LOOP", "LOCOMOTION"}
    metadata.exit_enabled = preset.get("exit", False)
    metadata.hold_last_frame = preset.get("hold", False)
    blend = preset.get("blend", 3)
    metadata.blend_in_ticks = blend
    metadata.blend_out_ticks = blend
    metadata.fallback_blend_ticks = blend
