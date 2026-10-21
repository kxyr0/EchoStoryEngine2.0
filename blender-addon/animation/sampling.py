from __future__ import annotations

import math

from mathutils import Euler, Matrix, Vector

from ..rig.schema import LEGACY_BONE_ORDER, LEGACY_REST_LOCAL_POSITION, resolve_skeleton_schema
from .model import ExportKey, ExportTrack


UNIT = 0.0254
POSITION_EPSILON = 1e-5
ROTATION_EPSILON_DEGREES = 1e-4
SCALE_EPSILON = 1e-5
SHEAR_EPSILON = 1e-4
BONE_ORDER = LEGACY_BONE_ORDER
REST_POSITION = {name: Vector(value) for name, value in LEGACY_REST_LOCAL_POSITION.items()}
BLENDER_TO_ECHO = Matrix(((1.0, 0.0, 0.0), (0.0, 0.0, -1.0), (0.0, 1.0, 0.0)))
ECHO_TO_BLENDER = BLENDER_TO_ECHO.transposed()


def validate_rig(armature):
    schema = resolve_skeleton_schema(armature)
    if tuple(schema.runtime_names) != tuple(BONE_ORDER) or tuple(schema.source_names) != tuple(BONE_ORDER):
        raise RuntimeError("Animation export requires JDH Humanoid v2 with body_lower + body.")
    return schema


def _rotation(matrix: Matrix) -> Matrix:
    return matrix.to_quaternion().to_matrix()


def _local_pose_and_rest(armature, bone_name):
    pose_bone = armature.pose.bones[bone_name]
    data_bone = armature.data.bones[bone_name]
    if pose_bone.parent is None:
        return pose_bone.matrix.copy(), data_bone.matrix_local.copy(), Matrix.Identity(3)
    return (
        pose_bone.parent.matrix.inverted_safe() @ pose_bone.matrix,
        data_bone.parent.matrix_local.inverted_safe() @ data_bone.matrix_local,
        _rotation(data_bone.parent.matrix_local),
    )


def _scale(armature, bone_name, pose_local, rest_local):
    pose, rest = pose_local.to_scale(), rest_local.to_scale()
    local = Vector(tuple(pose[i] / rest[i] if abs(rest[i]) > SCALE_EPSILON else 1.0 for i in range(3)))
    if any(value <= SCALE_EPSILON for value in local):
        raise RuntimeError(f"Bone '{bone_name}' uses zero or negative scale.")
    basis = _rotation(armature.data.bones[bone_name].matrix_local)
    matrix = BLENDER_TO_ECHO @ (basis @ Matrix.Diagonal(local) @ basis.transposed()) @ ECHO_TO_BLENDER
    shear = max(abs(matrix[row][column]) for row in range(3) for column in range(3) if row != column)
    if shear > SHEAR_EPSILON:
        raise RuntimeError(f"Bone '{bone_name}' scale produces unsupported shear ({shear:.6f}).")
    return tuple(matrix[index][index] for index in range(3))


def sample_pose(armature, bone_name: str, previous_euler: Euler | None, export_scale: bool):
    pose_local, rest_local, parent_rotation = _local_pose_and_rest(armature, bone_name)
    delta = parent_rotation @ (pose_local.translation - rest_local.translation)
    position = REST_POSITION[bone_name] + (BLENDER_TO_ECHO @ delta) / UNIT
    rotation_delta = _rotation(pose_local) @ _rotation(rest_local).transposed()
    rotation_blender = parent_rotation @ rotation_delta @ parent_rotation.transposed()
    rotation_matrix = BLENDER_TO_ECHO @ rotation_blender @ ECHO_TO_BLENDER
    euler = rotation_matrix.to_euler("XYZ") if previous_euler is None else rotation_matrix.to_euler("XYZ", previous_euler)
    rotation = tuple(math.degrees(value) for value in euler)
    scale = _scale(armature, bone_name, pose_local, rest_local)
    if not export_scale and any(abs(value - 1.0) > SCALE_EPSILON for value in scale):
        raise RuntimeError(f"Bone '{bone_name}' contains animated scale while Export Scale is disabled.")
    return (tuple(position), rotation, scale if export_scale else (1.0, 1.0, 1.0)), euler.copy()


def mask_bones(metadata) -> tuple[str, ...]:
    upper = {"body_lower", "body", "head", "left_shoulder", "left_upper_arm", "left_forearm", "right_shoulder", "right_upper_arm", "right_forearm"}
    lower = {"root", "left_thigh", "left_shin", "right_thigh", "right_shin"}
    if metadata.channel == "UPPER_BODY":
        allowed = upper
    elif metadata.channel == "LOWER_BODY":
        allowed = lower
    elif metadata.channel == "HEAD":
        allowed = {"head"}
    elif metadata.channel == "FACE":
        allowed = set()
    elif metadata.channel == "CUSTOM":
        allowed = {value.strip() for value in metadata.custom_bones.split(",") if value.strip()}
    else:
        allowed = set(BONE_ORDER)
    return tuple(bone for bone in BONE_ORDER if bone in allowed)


def base_value(bone: str):
    return tuple(REST_POSITION[bone]), (0.0, 0.0, 0.0), (1.0, 1.0, 1.0)


def differs(value, bone: str) -> bool:
    base = base_value(bone)
    return any(abs(a - b) > POSITION_EPSILON for a, b in zip(value[0], base[0])) or any(abs(value[1][i]) > ROTATION_EPSILON_DEGREES for i in range(3)) or any(abs(value[2][i] - 1.0) > SCALE_EPSILON for i in range(3))


def reduce_holds(keys: list[ExportKey]) -> list[ExportKey]:
    if len(keys) < 3:
        return keys
    result = [keys[0]]
    for index in range(1, len(keys) - 1):
        previous, current, following = keys[index - 1], keys[index], keys[index + 1]
        same = previous.position == current.position == following.position and previous.rotation == current.rotation == following.rotation and previous.scale == current.scale == following.scale
        if not same:
            result.append(current)
    result.append(keys[-1])
    return result


def sample_tracks(context, armature, frames: list[int], tick_by_frame: dict[int, int], metadata):
    bones = mask_bones(metadata)
    previous = {bone: None for bone in bones}
    values = {bone: [] for bone in bones}
    original_frame = context.scene.frame_current
    try:
        for frame in frames:
            context.scene.frame_set(frame)
            context.view_layer.update()
            for bone in bones:
                value, previous[bone] = sample_pose(armature, bone, previous[bone], metadata.export_scale)
                values[bone].append((frame, value))
    finally:
        context.scene.frame_set(original_frame)
        context.view_layer.update()
    tracks = []
    for bone in bones:
        if bone == metadata.root_bone and metadata.root_motion_mode == "DISABLED":
            continue
        if not any(differs(value, bone) for _frame, value in values[bone]):
            continue
        keys = [ExportKey(tick_by_frame[frame], value[0], value[1], value[2], metadata.interpolation) for frame, value in values[bone]]
        if metadata.optimize_holds:
            keys = reduce_holds(keys)
        tracks.append(ExportTrack(bone, tuple(keys)))
    return tuple(tracks), values
