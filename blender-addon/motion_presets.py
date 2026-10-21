from __future__ import annotations

import json
import math
from dataclasses import dataclass
from typing import Callable

import bpy
from mathutils import Quaternion, Vector

from .animation.exporter import ACTION_EXPORT_SETTINGS_KEY, get_active_armature
from .rig.schema import resolve_skeleton_schema


UNIT = 0.0254


@dataclass(frozen=True)
class BoneRole:
    role: str
    runtime_names: tuple[str, ...]
    source_names: tuple[str, ...]


ROLES = (
    BoneRole("root", ("root",), ("Root2", "root")),
    BoneRole("body_lower", ("body_lower",), ("Body_Lower", "Spine_Start3", "body_lower")),
    BoneRole("body", ("body",), ("Body_Upper", "Spine_End2", "body")),
    BoneRole("head", ("head",), ("Head_Control", "Head", "head")),
    BoneRole("left_shoulder", ("left_shoulder",), ("L_Shoulder", "left_shoulder")),
    BoneRole("left_upper_arm", ("left_upper_arm",), ("L_Arm", "left_upper_arm")),
    BoneRole("left_forearm", ("left_forearm",), ("L_Arm_Bend", "left_forearm")),
    BoneRole("left_hand", ("left_hand",), ("L_Wrist", "left_hand")),
    BoneRole("right_shoulder", ("right_shoulder",), ("R_Shoulder", "right_shoulder")),
    BoneRole("right_upper_arm", ("right_upper_arm",), ("R_Arm", "right_upper_arm")),
    BoneRole("right_forearm", ("right_forearm",), ("R_Arm_Bend", "right_forearm")),
    BoneRole("right_hand", ("right_hand",), ("R_Wrist", "right_hand")),
    BoneRole("left_thigh", ("left_thigh",), ("L_Leg", "left_thigh")),
    BoneRole("left_shin", ("left_shin",), ("L_Leg_Bend", "left_shin")),
    BoneRole("left_foot", ("left_foot",), ("L_Ankle", "left_foot")),
    BoneRole("right_thigh", ("right_thigh",), ("R_Leg", "right_thigh")),
    BoneRole("right_shin", ("right_shin",), ("R_Leg_Bend", "right_shin")),
    BoneRole("right_foot", ("right_foot",), ("R_Ankle", "right_foot")),
)


# Modern presets target the split-torso rig. TheRig adds hands/feet on top.
REQUIRED_CORE = {
    "root", "body_lower", "body", "head",
    "left_shoulder", "left_upper_arm", "left_forearm",
    "right_shoulder", "right_upper_arm", "right_forearm",
    "left_thigh", "left_shin", "right_thigh", "right_shin",
}


def _active_pose_bones(armature) -> dict[str, bpy.types.PoseBone]:
    return {bone.name: bone for bone in armature.pose.bones}


def _role_map(armature) -> dict[str, bpy.types.PoseBone]:
    pose_by_name = _active_pose_bones(armature)
    by_runtime = {}
    try:
        schema = resolve_skeleton_schema(armature)
        for entry in schema.bones:
            pose = pose_by_name.get(entry.source_name)
            if pose is not None:
                by_runtime[entry.runtime_name] = pose
    except Exception:
        pass

    result: dict[str, bpy.types.PoseBone] = {}
    used = set()
    for spec in ROLES:
        # Prefer an explicit authoring/control candidate when the rig exposes one.
        # This matters on TheRig: e.g. Head_Control should be keyed instead of
        # fighting the evaluated Head deform bone. If no control-like candidate
        # exists, fall back to the exact runtime source resolved by the schema.
        candidates = []
        for order, source_name in enumerate(spec.source_names):
            candidate = pose_by_name.get(source_name)
            if candidate is None:
                continue
            data_bone = candidate.bone
            score = -order
            if not bool(getattr(data_bone, "use_deform", False)):
                score += 100
            if "control" in candidate.name.lower():
                score += 40
            if len(candidate.constraints) == 0:
                score += 10
            candidates.append((score, candidate))

        bone = max(candidates, key=lambda item: item[0])[1] if candidates else None
        if bone is None:
            for runtime_name in spec.runtime_names:
                candidate = by_runtime.get(runtime_name)
                if candidate is not None:
                    bone = candidate
                    break
        if bone is not None and bone.name not in used:
            result[spec.role] = bone
            used.add(bone.name)
    return result


def _frames_for_seconds(context, seconds: float, divisions: int = 8) -> tuple[int, int, list[tuple[int, float]]]:
    fps = context.scene.render.fps / max(context.scene.render.fps_base, 1e-6)
    duration = max(divisions, int(round(fps * seconds)))
    duration = max(divisions, int(round(duration / divisions)) * divisions)
    start = 1
    end = start + duration
    keys = []
    for index in range(divisions + 1):
        phase = index / divisions
        frame = start + round(duration * phase)
        keys.append((frame, phase))
    return start, end, keys


def _q_axis(axis: tuple[float, float, float], degrees: float) -> Quaternion:
    return Quaternion(Vector(axis), math.radians(degrees))


def _armature_space_delta(pose_bone, pitch: float, yaw: float, roll: float) -> Quaternion:
    # Blender character convention used by Echo rigs: X right, -Y forward, Z up.
    # pitch -> X, yaw -> Z, roll -> Y. Convert those armature-space deltas into
    # the rest-local basis of the actual bone, so differently oriented bones do
    # not need per-rig magic Euler signs.
    armature_delta = (
        _q_axis((0.0, 0.0, 1.0), yaw)
        @ _q_axis((0.0, 1.0, 0.0), roll)
        @ _q_axis((1.0, 0.0, 0.0), pitch)
    )
    rest = pose_bone.bone.matrix_local.to_quaternion().normalized()
    return (rest.inverted() @ armature_delta @ rest).normalized()


def _key_rotation(pose_bone, frame: int, pitch: float = 0.0, yaw: float = 0.0, roll: float = 0.0) -> None:
    pose_bone.rotation_mode = "QUATERNION"
    pose_bone.rotation_quaternion = _armature_space_delta(pose_bone, pitch, yaw, roll)
    pose_bone.keyframe_insert(data_path="rotation_quaternion", frame=frame, group=pose_bone.name)


def _key_location(pose_bone, frame: int, x: float = 0.0, y: float = 0.0, z: float = 0.0) -> None:
    armature_delta = Vector((x, y, z))
    rest = pose_bone.bone.matrix_local.to_quaternion().normalized()
    pose_bone.location = rest.inverted() @ armature_delta
    pose_bone.keyframe_insert(data_path="location", frame=frame, group=pose_bone.name)


def _smooth_action(action) -> None:
    # Blender 4.3 still exposes action.fcurves for normal Actions. Keep this
    # best-effort so a future Blender API change cannot break preset creation.
    try:
        for fcurve in action.fcurves:
            for key in fcurve.keyframe_points:
                key.interpolation = "BEZIER"
                key.handle_left_type = "AUTO_CLAMPED"
                key.handle_right_type = "AUTO_CLAMPED"
    except Exception:
        pass


def _store_loop_export_defaults(action) -> None:
    # Quick Export immediately knows that these authored presets are true loops.
    # Folder/root are intentionally omitted so the user's addon preferences win.
    payload = {
        "animation_behavior": "LOOP",
        "range_source": "ACTION",
        "sample_step": 1,
        "main_duration_ticks": 0,
        "end_hold_ticks": 0,
        "export_scale": True,
        "optimize_holds": True,
        "interpolation": "SMOOTH",
        "loop_seam": "SMOOTH",
        "show_quick_help": False,
        "show_paths_preview": False,
        "show_sampling_options": False,
        "show_advanced_options": False,
    }
    action[ACTION_EXPORT_SETTINGS_KEY] = json.dumps(payload, ensure_ascii=False, separators=(",", ":"))
    action["echo_motion_preset"] = "modern"
    action["echo_motion_loop"] = True


def _create_action(armature, name: str) -> bpy.types.Action:
    action = bpy.data.actions.new(name=name)
    action.use_fake_user = True
    armature.animation_data_create().action = action
    return action


def _reset_role_pose(role_bones: dict[str, bpy.types.PoseBone]) -> None:
    for bone in role_bones.values():
        bone.location = Vector((0.0, 0.0, 0.0))
        bone.scale = Vector((1.0, 1.0, 1.0))
        bone.rotation_mode = "QUATERNION"
        bone.rotation_quaternion = Quaternion((1.0, 0.0, 0.0, 0.0))


def _walk_pose(phase: float) -> dict[str, tuple[float, float, float]]:
    a = math.tau * phase
    c = math.cos(a)
    s = math.sin(a)
    s2 = math.sin(a * 2.0)

    left_knee = 7.0 + 21.0 * max(0.0, -s)
    right_knee = 7.0 + 21.0 * max(0.0, s)

    return {
        "body_lower": (1.0 + 0.7 * s2, 3.2 * c, 1.0 * s),
        "body": (2.2 + 0.9 * s2, -4.0 * c, -1.4 * s),
        "head": (-0.7 - 0.45 * s2, 1.4 * c, 0.45 * s),
        "left_shoulder": (-1.0 * c, -1.8 * c, 2.0 + 1.0 * s),
        "right_shoulder": (1.0 * c, 1.8 * c, -2.0 - 1.0 * s),
        "left_upper_arm": (-22.0 * c - 1.5, 2.0 * s, 3.0),
        "right_upper_arm": (22.0 * c - 1.5, -2.0 * s, -3.0),
        "left_forearm": (12.0 + 7.0 * max(0.0, c), -1.0 * s, 1.2),
        "right_forearm": (12.0 + 7.0 * max(0.0, -c), 1.0 * s, -1.2),
        "left_hand": (-2.2 * c, 1.3 * s, -1.5),
        "right_hand": (2.2 * c, -1.3 * s, 1.5),
        "left_thigh": (27.0 * c, -1.2 * s, 1.5),
        "right_thigh": (-27.0 * c, 1.2 * s, -1.5),
        "left_shin": (-left_knee, 0.0, 0.6 * s),
        "right_shin": (-right_knee, 0.0, -0.6 * s),
        "left_foot": (-8.0 * c + 4.0 * s, 0.0, -0.8 * s),
        "right_foot": (8.0 * c - 4.0 * s, 0.0, 0.8 * s),
    }


def _idle_pose(phase: float) -> dict[str, tuple[float, float, float]]:
    a = math.tau * phase
    s = math.sin(a)
    c = math.cos(a)
    s2 = math.sin(a * 2.0 + 0.35)
    c2 = math.cos(a * 2.0 - 0.2)

    return {
        "body_lower": (0.20 * s2, 0.70 * s, 0.45 * c),
        "body": (0.45 + 0.35 * s, -0.85 * s, -0.55 * c),
        "head": (-0.18 - 0.30 * s, 0.95 * s + 0.20 * s2, 0.25 * c2),
        "left_shoulder": (-0.25 * s, -0.35 * s, 0.80 + 0.30 * c),
        "right_shoulder": (0.20 * s, 0.30 * s, -0.45 - 0.25 * c),
        "left_upper_arm": (-0.75 + 0.80 * s, 0.35 * c, 1.30),
        "right_upper_arm": (-0.35 - 0.65 * s, -0.30 * c, -0.85),
        "left_forearm": (-4.5 + 1.2 * s2, 0.20 * s, 0.45),
        "right_forearm": (-3.2 - 1.0 * s2, -0.20 * s, -0.35),
        "left_hand": (0.55 * s2, 0.25 * c, -0.40),
        "right_hand": (-0.45 * s2, -0.20 * c, 0.35),
        "left_thigh": (0.50 * s, -0.20 * c, 0.18),
        "right_thigh": (-0.40 * s, 0.18 * c, -0.15),
        "left_shin": (-1.0 - 0.65 * max(0.0, s), 0.0, 0.12 * c),
        "right_shin": (-0.75 - 0.55 * max(0.0, -s), 0.0, -0.10 * c),
        "left_foot": (0.22 * s, 0.0, -0.15 * c),
        "right_foot": (-0.18 * s, 0.0, 0.12 * c),
    }


def _root_motion(kind: str, phase: float) -> tuple[float, float, float, tuple[float, float, float]]:
    a = math.tau * phase
    if kind == "WALK":
        # Two vertical pulses per stride, tiny lateral weight transfer, no
        # forward displacement: locomotion stays controlled by the game.
        x = 0.11 * UNIT * math.sin(a)
        y = 0.0
        z = 0.17 * UNIT * (0.5 - 0.5 * math.cos(a * 2.0))
        rotation = (0.30 * math.sin(a * 2.0), 1.1 * math.cos(a), 0.55 * math.sin(a))
        return x, y, z, rotation

    x = 0.045 * UNIT * math.sin(a)
    y = 0.018 * UNIT * math.sin(a * 2.0 + 0.4)
    z = 0.040 * UNIT * math.sin(a)
    rotation = (0.10 * math.sin(a * 2.0), 0.35 * math.sin(a), 0.18 * math.cos(a))
    return x, y, z, rotation


def _generate(context, kind: str, action_name: str) -> tuple[bpy.types.Action, int, list[str]]:
    armature = get_active_armature(context)
    if armature is None:
        raise RuntimeError("Выберите Armature Echo")

    roles = _role_map(armature)
    missing_core = sorted(REQUIRED_CORE - set(roles))
    if missing_core:
        raise RuntimeError(
            "Не найден основной набор костей для пресета: " + ", ".join(missing_core)
        )

    _reset_role_pose(roles)
    action = _create_action(armature, action_name)
    seconds = 0.90 if kind == "WALK" else 4.00
    start, end, frames = _frames_for_seconds(context, seconds, divisions=8)
    pose_fn: Callable[[float], dict[str, tuple[float, float, float]]] = _walk_pose if kind == "WALK" else _idle_pose

    for frame, phase in frames:
        root = roles["root"]
        x, y, z, root_rot = _root_motion(kind, phase)
        _key_location(root, frame, x, y, z)
        _key_rotation(root, frame, *root_rot)

        pose = pose_fn(phase)
        for role, rotation in pose.items():
            bone = roles.get(role)
            if bone is not None:
                _key_rotation(bone, frame, *rotation)

    _smooth_action(action)
    _store_loop_export_defaults(action)
    action["echo_motion_kind"] = kind.lower()
    action["echo_motion_frames"] = int(end - start)
    action["echo_motion_roles"] = ",".join(sorted(roles))
    context.scene.frame_set(start)
    context.view_layer.update()
    return action, len(roles), sorted(roles)


class ECHO_OT_create_modern_walk(bpy.types.Operator):
    bl_idname = "echo.create_modern_walk"
    bl_label = "Создать Modern Walk"
    bl_description = "Создать современный Minecraft-style walk: таз, корпус, голова, плечи, локти, кисти, колени и стопы работают вместе"
    bl_options = {"REGISTER", "UNDO"}

    @classmethod
    def poll(cls, context):
        return get_active_armature(context) is not None

    def execute(self, context):
        try:
            action, count, roles = _generate(context, "WALK", "modern_walk")
            self.report({"INFO"}, f"Создан {action.name}: {count} основных костей, настоящий Loop")
            print("Echo Modern Walk bones:", ", ".join(roles))
            return {"FINISHED"}
        except Exception as error:
            self.report({"ERROR"}, str(error))
            return {"CANCELLED"}


class ECHO_OT_create_modern_idle(bpy.types.Operator):
    bl_idname = "echo.create_modern_idle"
    bl_label = "Создать Modern Idle"
    bl_description = "Создать живой Minecraft-style idle: дыхание, перенос веса, микродвижения головы, плеч, рук, коленей и стоп"
    bl_options = {"REGISTER", "UNDO"}

    @classmethod
    def poll(cls, context):
        return get_active_armature(context) is not None

    def execute(self, context):
        try:
            action, count, roles = _generate(context, "IDLE", "modern_idle")
            self.report({"INFO"}, f"Создан {action.name}: {count} основных костей, настоящий Loop")
            print("Echo Modern Idle bones:", ", ".join(roles))
            return {"FINISHED"}
        except Exception as error:
            self.report({"ERROR"}, str(error))
            return {"CANCELLED"}


MOTION_PRESET_CLASSES = (
    ECHO_OT_create_modern_walk,
    ECHO_OT_create_modern_idle,
)
