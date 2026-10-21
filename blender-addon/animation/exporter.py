from __future__ import annotations

import json

# Canonical package build. Install the ZIP, not this file directly.

bl_info = {
    "name": "Echo Animation Exporter",
    "author": "No Echo",
    "version": (3, 3, 0),
    "blender": (4, 3, 0),
    "location": "File > Export > Echo Animation (.es); View3D > Sidebar > Echo",
    "description": "Exact matrix EchoScript export for full TheRig 2.6 deform skeletons",
    "category": "Import-Export",
}

import math
import re
from dataclasses import dataclass
from pathlib import Path

import bpy
from bpy.props import BoolProperty, EnumProperty, IntProperty, StringProperty
from mathutils import Euler, Matrix, Vector

from ..rig.schema import (
    LEGACY_BONE_ORDER,
    LEGACY_REST_LOCAL_POSITION,
    LEGACY_RIG_ID,
    resolve_skeleton_schema,
)
from ..rig.coordinate_converter import convert_local_matrix
from ..rig.matrix_converter import source_local_bind

ADDON_PACKAGE = (__package__ or __name__).split(".", 1)[0]
DEFAULT_OUTPUT_ROOT = (
    r"C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances"
    r"\Форма Мысли\minecraft\story\content-pack\jdh"
    r"\echo_animation"
)
DEFAULT_RESOURCE_FOLDER = "chapter_1/family"

# Export settings are saved on the Blender Action itself, so each animation
# remembers its own ranges/timings/options across repeated exports and .blend reloads.
ACTION_EXPORT_SETTINGS_KEY = "echo_export_settings_v1"

# When an Action has never been exported, only these generic preferences may
# be inherited from the last export. IDs and frame ranges must never leak from
# a different animation.
GLOBAL_DIALOG_RESTORE_FIELDS = {
    # Only genuinely generic preferences are inherited by a brand-new Action.
    # Behaviour, frame ranges and timings are Action-specific and must not leak
    # from a completely different animation.
    "resource_folder",
    "sample_step",
    "export_scale",
    "optimize_holds",
    "interpolation",
    "loop_seam",
    "show_quick_help",
    "show_paths_preview",
    "show_sampling_options",
    "show_advanced_options",
}

ANIMATION_BEHAVIOR_ITEMS = (
    ("AUTO", "Авто", "По имени Action: idle/walk/run/sprint/sneak/crawl = Цикл, остальное = Один раз"),
    ("ONE_SHOT", "Разово", "Проиграть Action один раз. Enter и Exit не создаются"),
    ("LOOP", "Цикл", "Повторять Action до команды Stop. Идеально для idle/walk/run; Enter и Exit не создаются"),
    ("HOLD", "Hold", "Проиграть Action один раз и удерживать последнюю позу до Stop. Без огромных holdTicks"),
    ("LIFECYCLE", "Фазы", "Расширенный режим Enter -> Main -> Exit"),
)

LIFECYCLE_MAIN_MODE_ITEMS = (
    ("ONCE", "Разово", "Main проигрывается один раз, затем запускается Exit"),
    ("LOOP_UNTIL_STOP", "Цикл до Stop", "Main повторяется до Stop, затем запускается Exit на границе цикла"),
    ("HOLD_UNTIL_STOP", "Hold до Stop", "Main проигрывается один раз, последняя поза удерживается до Stop, затем запускается Exit"),
)

LOOP_NAME_HINTS = {
    "idle", "walk", "run", "sprint", "sneak", "crawl", "jog", "locomotion",
}

RESOURCE_NAMESPACE = "jdh"
SCRIPT_NAMESPACE = "JDH.Animation"
RIG_RESOURCE_ID = LEGACY_RIG_ID
UNIT = 0.0254
POSITION_EPSILON = 1e-5
ROTATION_EPSILON_DEGREES = 1e-4
SCALE_EPSILON = 1e-5
SHEAR_EPSILON = 1e-4
PROFILE_SHEAR_EPSILON = 1e-2
MATRIX_EPSILON = 1e-6
FRAMES_PER_TIMER_TICK = 3

# Coordinate basis used by the runtime renderer:
# Blender X/right, Y/forward, Z/up -> Echo X/right, Y/down, Z/forward.
#
# IMPORTANT: this is a proper +90 degree X-axis basis rotation.  Older
# exporter builds used a Y/Z reflection here and then tried to repair the
# resulting Euler angles with per-axis sign flips.  That only matched simple
# single-axis poses and mirrored Z rotation on compound arm poses.
BLENDER_TO_ECHO = Matrix((
    (1.0, 0.0, 0.0),
    (0.0, 0.0, -1.0),
    (0.0, 1.0, 0.0),
))
ECHO_TO_BLENDER = BLENDER_TO_ECHO.transposed()

BONE_ORDER = LEGACY_BONE_ORDER

REST_LOCAL_POSITION = {
    name: Vector(position)
    for name, position in LEGACY_REST_LOCAL_POSITION.items()
}


@dataclass(frozen=True)
class PoseValue:
    position: tuple[float, float, float]
    rotation: tuple[float, float, float]
    scale: tuple[float, float, float]


@dataclass(frozen=True)
class MatrixPoseValue:
    matrix: tuple[float, ...]


PoseSample = PoseValue | MatrixPoseValue


def sanitize_animation_name(value: str) -> str:
    value = value.strip().lower()
    value = re.sub(r"\s+", "_", value)
    value = re.sub(r"[^a-z0-9_.-]+", "_", value)
    value = re.sub(r"_+", "_", value)
    return value.strip("_.-") or "my_animation"


def sanitize_resource_folder(value: str) -> str:
    value = value.strip().lower().replace("\\", "/")
    parts = []

    for raw_part in value.split("/"):
        raw_part = raw_part.strip()
        if not raw_part:
            continue

        part = re.sub(r"\s+", "_", raw_part)
        part = re.sub(r"[^a-z0-9_.-]+", "_", part)
        part = re.sub(r"_+", "_", part).strip("_.-")
        if part:
            parts.append(part)

    return "/".join(parts)


def join_resource_path(*parts: str) -> str:
    return "/".join(part.strip("/") for part in parts if part.strip("/"))


def runtime_animation_id(animation_id: str) -> str:
    """Return the Brigadier-safe playable ID. Storage folders never affect IDs."""
    return f"{RESOURCE_NAMESPACE}:{sanitize_animation_name(animation_id)}"


def runtime_clip_id(animation_id: str, phase: str) -> str:
    """Return a flat clip ID such as jdh:new_animation_main."""
    name = sanitize_animation_name(animation_id)
    return f"{RESOURCE_NAMESPACE}:{name}_{phase}"


def guess_animation_behavior(action_name: str) -> str:
    """Choose a sane default without forcing lifecycle boilerplate on animators.

    System pose Actions are static states, not one-shots. In particular,
    ``pose_sit`` is consumed automatically by runtime while an NPC is sitting,
    so exporting it as HOLD is the only sane default.
    """
    normalized = sanitize_animation_name(action_name)
    if normalized in {"pose_sit", "sit_pose"}:
        return "HOLD"
    tokens = {token for token in re.split(r"[_.-]+", normalized) if token}
    if tokens & LOOP_NAME_HINTS:
        return "LOOP"
    return "ONE_SHOT"


def runtime_main_mode(behavior: str, lifecycle_main_mode: str) -> str:
    if behavior == "LOOP":
        return "LOOP_UNTIL_STOP"
    if behavior == "HOLD":
        return "HOLD_UNTIL_STOP"
    if behavior == "LIFECYCLE":
        return lifecycle_main_mode
    return "ONCE"


def behavior_title(behavior: str) -> str:
    return {
        "ONE_SHOT": "Разово",
        "LOOP": "Цикл до Stop",
        "HOLD": "Hold до Stop",
        "LIFECYCLE": "Enter → Main → Exit",
    }.get(behavior, behavior)


def get_active_armature(context: bpy.types.Context) -> bpy.types.Object | None:
    obj = context.object
    if obj is not None and obj.type == "ARMATURE":
        return obj

    selected = [obj for obj in context.selected_objects if obj.type == "ARMATURE"]
    return selected[0] if len(selected) == 1 else None


def get_active_action(armature: bpy.types.Object):
    animation_data = armature.animation_data
    return animation_data.action if animation_data is not None else None


def validate_rig(armature: bpy.types.Object):
    schema = resolve_skeleton_schema(armature)
    if schema.legacy_compatible and (
        schema.runtime_names != BONE_ORDER or schema.source_names != BONE_ORDER
    ):
        raise RuntimeError(
            "EchoScript animation export requires JDH Humanoid v2 with body_lower + body. "
            "Use 'Разделить корпус на 2 кости' for an old v1 rig; arbitrary runtime skeletons can still "
            "be exported as *.skin.json v2."
        )
    return schema


def _profile_local_pose(armature, schema, bone_index: int) -> Matrix:
    entry = schema.bones[bone_index]
    child = armature.pose.bones[entry.source_name].matrix.copy()
    if entry.parent_index < 0:
        return child
    parent_name = schema.bones[entry.parent_index].source_name
    parent = armature.pose.bones[parent_name].matrix.copy()
    return parent.inverted_safe() @ child


def _matrix_pose_value(
    matrix: Matrix,
    bone_name: str,
    previous_euler: Euler | None,
    export_scale: bool,
) -> tuple[PoseValue, Euler]:
    location, rotation, scale = matrix.decompose()
    if any(component <= SCALE_EPSILON for component in scale):
        raise RuntimeError(
            f"Bone '{bone_name}' uses zero or negative evaluated scale "
            f"{tuple(round(v, 6) for v in scale)}"
        )
    reconstructed = Matrix.LocRotScale(location, rotation, scale)
    matrix_error = max(
        abs(float(matrix[row][column]) - float(reconstructed[row][column]))
        for row in range(4)
        for column in range(4)
    )
    if matrix_error > PROFILE_SHEAR_EPSILON:
        raise RuntimeError(
            f"Bone '{bone_name}' evaluated transform contains unsupported shear "
            f"({matrix_error:.6f})"
        )
    if previous_euler is None:
        euler = rotation.to_euler("XYZ")
    else:
        euler = rotation.to_euler("XYZ", previous_euler)
    scale_changed = any(abs(component - 1.0) > SCALE_EPSILON for component in scale)
    if scale_changed and not export_scale:
        raise RuntimeError(
            f"Bone '{bone_name}' contains evaluated scale. Enable 'Export scale'."
        )
    return (
        PoseValue(
            position=tuple(float(component) for component in location),
            rotation=tuple(math.degrees(component) for component in euler),
            scale=(
                tuple(float(component) for component in scale)
                if export_scale
                else (1.0, 1.0, 1.0)
            ),
        ),
        euler.copy(),
    )


def _exact_matrix_pose_value(matrix: Matrix, bone_name: str) -> MatrixPoseValue:
    values = tuple(
        float(matrix[row][column])
        for row in range(4)
        for column in range(4)
    )
    if not all(math.isfinite(value) for value in values):
        raise RuntimeError(f"Bone '{bone_name}' evaluated matrix contains a non-finite value")
    return MatrixPoseValue(values)


def sample_schema_pose_value(
    armature: bpy.types.Object,
    schema,
    bone_name: str,
    previous_euler: Euler | None,
    export_scale: bool,
) -> tuple[PoseSample, Euler | None]:
    index = schema.runtime_names.index(bone_name)
    local = convert_local_matrix(_profile_local_pose(armature, schema, index))
    if schema.legacy_compatible:
        return _matrix_pose_value(local, bone_name, previous_euler, export_scale)
    return _exact_matrix_pose_value(local, bone_name), None


def schema_base_pose_values(armature, schema, export_scale: bool) -> dict[str, PoseSample]:
    if schema.legacy_compatible:
        return {name: base_pose(name) for name in schema.runtime_names}
    result = {}
    for index, entry in enumerate(schema.bones):
        local = convert_local_matrix(source_local_bind(armature, schema, index))
        result[entry.runtime_name] = _exact_matrix_pose_value(local, entry.runtime_name)
    return result


def rotation_only(matrix: Matrix) -> Matrix:
    return matrix.to_quaternion().to_matrix()


def local_pose_and_rest(
    armature: bpy.types.Object,
    bone_name: str,
) -> tuple[Matrix, Matrix, Matrix]:
    pose_bone = armature.pose.bones[bone_name]
    data_bone = armature.data.bones[bone_name]

    if pose_bone.parent is None:
        pose_local = pose_bone.matrix.copy()
        rest_local = data_bone.matrix_local.copy()
        parent_rest_rotation = Matrix.Identity(3)
    else:
        pose_local = pose_bone.parent.matrix.inverted_safe() @ pose_bone.matrix
        rest_local = (
            data_bone.parent.matrix_local.inverted_safe()
            @ data_bone.matrix_local
        )
        parent_rest_rotation = rotation_only(data_bone.parent.matrix_local)

    return pose_local, rest_local, parent_rest_rotation


def blender_vector_to_echo_units(vector: Vector) -> Vector:
    return (BLENDER_TO_ECHO @ vector) / UNIT


def blender_rotation_to_echo(matrix: Matrix) -> Matrix:
    return BLENDER_TO_ECHO @ matrix @ ECHO_TO_BLENDER


def evaluated_echo_scale(
    armature: bpy.types.Object,
    bone_name: str,
    pose_local: Matrix,
    rest_local: Matrix,
) -> tuple[float, float, float]:
    """Convert evaluated Blender bone scale into Echo model-aligned axes.

    Echo bones have no authored rest rotation. Blender bones do, because their
    visible octahedrons point along each segment. We therefore treat scale as
    a tensor, rotate it out of the Blender rest-bone basis, and then apply the
    Blender Z-up -> Echo Y-up axis mapping.
    """
    pose_scale = pose_local.to_scale()
    rest_scale = rest_local.to_scale()

    local_scale = Vector((
        pose_scale[index] / rest_scale[index]
        if abs(rest_scale[index]) > SCALE_EPSILON
        else 1.0
        for index in range(3)
    ))

    if any(component <= SCALE_EPSILON for component in local_scale):
        raise RuntimeError(
            f"Bone '{bone_name}' uses zero or negative evaluated scale "
            f"{tuple(round(v, 6) for v in local_scale)}. "
            "Echo exporter currently supports positive scale only."
        )

    bone_rest_rotation = rotation_only(
        armature.data.bones[bone_name].matrix_local
    )
    scale_local_matrix = Matrix.Diagonal(local_scale)
    scale_blender_model = (
        bone_rest_rotation
        @ scale_local_matrix
        @ bone_rest_rotation.transposed()
    )
    scale_echo_matrix = (
        BLENDER_TO_ECHO
        @ scale_blender_model
        @ ECHO_TO_BLENDER
    )

    off_diagonal = max(
        abs(scale_echo_matrix[row][column])
        for row in range(3)
        for column in range(3)
        if row != column
    )
    if off_diagonal > SHEAR_EPSILON:
        raise RuntimeError(
            f"Bone '{bone_name}' scale produces shear ({off_diagonal:.6f}). "
            "Apply scale on the armature and animate scale only on clean "
            "rest-aligned Echo bones."
        )

    return tuple(
        scale_echo_matrix[index][index]
        for index in range(3)
    )


def sample_pose_value(
    armature: bpy.types.Object,
    bone_name: str,
    previous_euler: Euler | None,
    export_scale: bool,
) -> tuple[PoseValue, Euler]:
    pose_local, rest_local, parent_rest_rotation = local_pose_and_rest(
        armature,
        bone_name,
    )

    position_delta_parent = pose_local.translation - rest_local.translation
    position_delta_blender = parent_rest_rotation @ position_delta_parent
    position_echo = (
        REST_LOCAL_POSITION[bone_name]
        + blender_vector_to_echo_units(position_delta_blender)
    )

    pose_rotation = rotation_only(pose_local)
    rest_rotation = rotation_only(rest_local)
    rotation_delta_parent = pose_rotation @ rest_rotation.transposed()
    rotation_delta_blender = (
        parent_rest_rotation
        @ rotation_delta_parent
        @ parent_rest_rotation.transposed()
    )
    rotation_echo_matrix = blender_rotation_to_echo(rotation_delta_blender)

    if previous_euler is None:
        rotation_echo = rotation_echo_matrix.to_euler("XYZ")
    else:
        rotation_echo = rotation_echo_matrix.to_euler("XYZ", previous_euler)

    # The basis conversion above already produces the exact Echo-space
    # rotation.  Do not apply post-Euler sign corrections: changing one Euler
    # component after decomposition changes the represented matrix whenever
    # multiple axes are involved.
    rotation_degrees = tuple(
        math.degrees(component)
        for component in rotation_echo
    )

    scale_echo = evaluated_echo_scale(
        armature,
        bone_name,
        pose_local,
        rest_local,
    )
    scale_changed = any(
        abs(component - 1.0) > SCALE_EPSILON
        for component in scale_echo
    )
    if scale_changed and not export_scale:
        raise RuntimeError(
            f"Bone '{bone_name}' contains animated scale. "
            "Enable 'Export scale' or remove the scale keys."
        )

    value = PoseValue(
        position=tuple(position_echo),
        rotation=rotation_degrees,
        scale=scale_echo if export_scale else (1.0, 1.0, 1.0),
    )
    return value, rotation_echo.copy()


def base_pose(bone_name: str) -> PoseValue:
    return PoseValue(
        position=tuple(REST_LOCAL_POSITION[bone_name]),
        rotation=(0.0, 0.0, 0.0),
        scale=(1.0, 1.0, 1.0),
    )


def differs_from_base(
    value: PoseSample,
    bone_name: str,
    base_by_bone: dict[str, PoseSample] | None = None,
) -> bool:
    base = base_pose(bone_name) if base_by_bone is None else base_by_bone[bone_name]
    if isinstance(value, MatrixPoseValue) or isinstance(base, MatrixPoseValue):
        if not isinstance(value, MatrixPoseValue) or not isinstance(base, MatrixPoseValue):
            return True
        return any(abs(a - b) > MATRIX_EPSILON for a, b in zip(value.matrix, base.matrix))
    return (
        any(abs(a - b) > POSITION_EPSILON for a, b in zip(value.position, base.position))
        or any(abs(component) > ROTATION_EPSILON_DEGREES for component in value.rotation)
        or any(abs(a - b) > SCALE_EPSILON for a, b in zip(value.scale, base.scale))
    )


def clean_number(value: float, digits: int = 6) -> str:
    if abs(value) < 0.5 * (10 ** -digits):
        value = 0.0
    text = f"{value:.{digits}f}".rstrip("0").rstrip(".")
    if text in {"-0", "0", ""}:
        return "0.0"
    if "." not in text:
        text += ".0"
    return text


def vector_call(name: str, values: tuple[float, float, float]) -> str:
    x, y, z = (clean_number(value) for value in values)
    return f'.{name}("{x}", "{y}", "{z}")'


def progress_text(index: int, count: int) -> str:
    if index == 0:
        return "0.0"
    if index == count - 1:
        return "1.0"
    return clean_number(index / (count - 1), digits=8)


def progress_value(value: float) -> str:
    if value <= 0.0:
        return "0.0"
    if value >= 1.0:
        return "1.0"
    return clean_number(value, digits=8)


def pose_values_close(left: PoseSample, right: PoseSample) -> bool:
    if isinstance(left, MatrixPoseValue) or isinstance(right, MatrixPoseValue):
        return (
            isinstance(left, MatrixPoseValue)
            and isinstance(right, MatrixPoseValue)
            and all(abs(a - b) <= MATRIX_EPSILON for a, b in zip(left.matrix, right.matrix))
        )
    return (
        all(
            abs(a - b) <= POSITION_EPSILON
            for a, b in zip(left.position, right.position)
        )
        and all(
            abs(a - b) <= ROTATION_EPSILON_DEGREES
            for a, b in zip(left.rotation, right.rotation)
        )
        and all(
            abs(a - b) <= SCALE_EPSILON
            for a, b in zip(left.scale, right.scale)
        )
    )



def pose_debug_text(value: PoseSample) -> str:
    if isinstance(value, MatrixPoseValue):
        return "matrix=(" + ", ".join(f"{component:.6f}" for component in value.matrix) + ")"
    return (
        f"position={tuple(round(v, 6) for v in value.position)}, "
        f"rotation={tuple(round(v, 4) for v in value.rotation)}, "
        f"scale={tuple(round(v, 6) for v in value.scale)}"
    )

def reduce_redundant_hold_frames(
    frames: list[tuple[str, PoseSample, str | None]],
) -> list[tuple[str, PoseSample, str | None]]:
    """Remove only interior keys from perfectly constant runs.

    This is deliberately conservative: it never approximates moving curves,
    so LINEAR/SMOOTH/CUBIC motion keeps its sampled shape.
    """
    if len(frames) <= 2:
        return frames

    reduced = [frames[0]]
    for index in range(1, len(frames) - 1):
        previous_value = frames[index - 1][1]
        current_value = frames[index][1]
        next_value = frames[index + 1][1]

        if (
            pose_values_close(previous_value, current_value)
            and pose_values_close(current_value, next_value)
        ):
            continue

        reduced.append(frames[index])

    reduced.append(frames[-1])
    return reduced


def pascal_case(value: str) -> str:
    result = "".join(
        piece[:1].upper() + piece[1:]
        for piece in re.split(r"[^A-Za-z0-9]+", value)
        if piece
    )
    if not result:
        raise RuntimeError("Could not create an EchoScript class name.")
    return "Animation" + result if result[0].isdigit() else result


def render_frame(
    progress: str,
    value: PoseSample,
    interpolation: str | None,
) -> str:
    interpolation_arg = (
        f', interpolation: "{interpolation}"'
        if interpolation is not None
        else ""
    )
    frame = f'Frame("{progress}"{interpolation_arg})'
    if isinstance(value, MatrixPoseValue):
        values = ", ".join(f'"{clean_number(component, digits=8)}"' for component in value.matrix)
        return frame + f".Matrix({values})"
    return (
        frame
        + vector_call("Position", value.position)
        + vector_call("Rotation", value.rotation)
        + vector_call("Scale", value.scale)
    )


def render_clip(
    class_name: str,
    resource_id: str,
    tracks: dict[str, list[tuple[str, PoseSample, str | None]]],
    loop_seam: str,
    rig_resource_id: str = RIG_RESOURCE_ID,
    bone_order: tuple[str, ...] = BONE_ORDER,
) -> str:
    track_blocks: list[str] = []

    for bone_name in bone_order:
        frames = tracks.get(bone_name)
        if frames is None:
            continue

        frame_lines = []
        for index, (progress, value, interpolation) in enumerate(frames):
            suffix = "," if index < len(frames) - 1 else ""
            frame_lines.append(
                "                    "
                + render_frame(progress, value, interpolation)
                + suffix
            )

        track_blocks.append(
            "                "
            + f'Track("{bone_name}", frames: [\n'
            + "\n".join(frame_lines)
            + "\n                ])"
        )

    joined_tracks = ",\n".join(track_blocks)
    return f'''namespace {SCRIPT_NAMESPACE};

class {class_name} : AnimationClipResource
{{
    public override AnimationClipDef Build()
    {{
        return AnimationClip("{resource_id}",
            rig: "{rig_resource_id}",
            schemaVersion: 3,
            loopSeam: "{loop_seam}",
            tracks: [
{joined_tracks}
            ]);
    }}
}}
'''


def render_animation(
    class_name: str,
    animation_resource_id: str,
    main_clip_id: str,
    main_duration_ticks: int,
    main_mode: str,
    enter_clip_id: str | None = None,
    enter_duration_ticks: int = 0,
    exit_clip_id: str | None = None,
    exit_duration_ticks: int = 0,
) -> str:
    """Render a direct MAIN animation or the full authored lifecycle.

    Direct animations deliberately have no fake Enter/Exit resources.
    """
    entry = (
        f'[Entry("body/idle", clip: "{enter_clip_id}", durationTicks: {enter_duration_ticks})]'
        if enter_clip_id else "[]"
    )
    if exit_clip_id:
        exit_points = '[ExitPoint("complete", progress: "1.0")]'
        exits = f'[Exit("complete", clip: "{exit_clip_id}", durationTicks: {exit_duration_ticks})]'
    else:
        exit_points = "[]"
        exits = "[]"

    return f'''// Generated by Echo Animation Exporter 3.3.1 RU
// Runtime animation ID: {animation_resource_id}
namespace {SCRIPT_NAMESPACE};

class {class_name} : AnimationResource
{{
    public override AnimationDef Build()
    {{
        return Animation("{animation_resource_id}",
            channels: [Channel("body")],
            sources: [Source("body/idle")],
            entries: {entry},
            main: Main(clip: "{main_clip_id}", mode: "{main_mode}", durationTicks: {main_duration_ticks},
                exitPoints: {exit_points}),
            exits: {exits},
            target: "body/idle",
            recovery: Recovery("BOUNDED", checkpointIntervalTicks: 4),
            replay: "REJECT_IF_RUNNING");
    }}
}}
'''

def write_text(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8", newline="\n")


class ECHO_OT_copy_text(bpy.types.Operator):
    bl_idname = "echo.copy_text"
    bl_label = "Копировать"
    bl_description = "Скопировать ID или готовую команду Echo в буфер обмена"
    bl_options = {"INTERNAL"}

    text: StringProperty(default="")

    def execute(self, context):
        context.window_manager.clipboard = self.text
        self.report({"INFO"}, f"Скопировано: {self.text}")
        return {"FINISHED"}


class ECHO_OT_open_export_root(bpy.types.Operator):
    bl_idname = "echo.open_export_root"
    bl_label = "Открыть папку экспорта"
    bl_description = "Открыть корневую папку экспорта из настроек аддона"
    bl_options = {"REGISTER"}

    def execute(self, context):
        addon = context.preferences.addons.get(ADDON_PACKAGE)
        output_root = DEFAULT_OUTPUT_ROOT
        if addon is not None:
            output_root = addon.preferences.output_root or DEFAULT_OUTPUT_ROOT
        try:
            path = Path(bpy.path.abspath(output_root)).expanduser().resolve()
            path.mkdir(parents=True, exist_ok=True)
            bpy.ops.wm.path_open(filepath=str(path))
        except Exception as error:
            self.report({"ERROR"}, f"Не удалось открыть папку экспорта: {error}")
            return {"CANCELLED"}
        self.report({"INFO"}, f"Открыта папка: {path}")
        return {"FINISHED"}


class ECHO_OT_open_last_export_location(bpy.types.Operator):
    bl_idname = "echo.open_last_export_location"
    bl_label = "Открыть папку последнего экспорта"
    bl_description = "Открыть папку, где лежит файл последней успешно экспортированной анимации"
    bl_options = {"REGISTER"}

    @classmethod
    def poll(cls, context):
        return bool(getattr(context.scene, "echo_last_export_path", ""))

    def execute(self, context):
        raw_path = getattr(context.scene, "echo_last_export_path", "")
        if not raw_path:
            self.report({"ERROR"}, "Последний экспорт ещё не выполнялся")
            return {"CANCELLED"}
        try:
            file_path = Path(bpy.path.abspath(raw_path)).expanduser().resolve()
            folder = file_path if file_path.is_dir() else file_path.parent
            folder.mkdir(parents=True, exist_ok=True)
            bpy.ops.wm.path_open(filepath=str(folder))
        except Exception as error:
            self.report({"ERROR"}, f"Не удалось открыть папку последнего экспорта: {error}")
            return {"CANCELLED"}
        return {"FINISHED"}


class ECHO_OT_open_addon_preferences(bpy.types.Operator):
    bl_idname = "echo.open_addon_preferences"
    bl_label = "Открыть настройки аддона"
    bl_description = "Открыть Preferences сразу на настройках Echo Animation Exporter"
    bl_options = {"REGISTER"}

    def execute(self, context):
        try:
            context.preferences.active_section = "ADDONS"
        except Exception:
            pass

        try:
            bpy.ops.screen.userpref_show('INVOKE_DEFAULT')
        except Exception as error:
            self.report({"ERROR"}, f"Не удалось открыть Preferences: {error}")
            return {"CANCELLED"}

        def focus_echo_addon():
            try:
                bpy.context.preferences.active_section = "ADDONS"
            except Exception:
                pass
            operator = getattr(bpy.ops.preferences, "addon_show", None)
            if operator is None:
                return None
            try:
                for window in bpy.context.window_manager.windows:
                    for area in window.screen.areas:
                        if area.type != "PREFERENCES":
                            continue
                        with bpy.context.temp_override(window=window, area=area):
                            operator(module=ADDON_PACKAGE)
                        return None
            except Exception:
                return None
            return None

        try:
            bpy.app.timers.register(focus_echo_addon, first_interval=0.15)
        except Exception:
            pass
        self.report({"INFO"}, "Открыты настройки Echo Animation Exporter")
        return {"FINISHED"}


class ECHO_OT_repeat_last_export(bpy.types.Operator):
    bl_idname = "echo.repeat_last_export"
    bl_label = "Повторить последний экспорт"
    bl_description = "Повторить последний успешный экспорт с тем же Armature, Action и параметрами"
    bl_options = {"REGISTER"}

    @classmethod
    def poll(cls, context):
        return bool(getattr(context.scene, "echo_last_export_json", ""))

    def execute(self, context):
        payload_raw = getattr(context.scene, "echo_last_export_json", "")
        if not payload_raw:
            self.report({"ERROR"}, "Нет данных о последнем успешном экспорте")
            return {"CANCELLED"}
        try:
            saved = json.loads(payload_raw)
        except Exception as error:
            self.report({"ERROR"}, f"Не удалось прочитать параметры последнего экспорта: {error}")
            return {"CANCELLED"}

        # 3.0.6 stores source objects separately. Flat payloads from an
        # intermediate 3.0.6 build are still accepted for compatibility.
        operator_payload = saved.get("operator", saved)
        armature_name = saved.get("armature_name", "")
        action_name = saved.get("action_name", "")

        if armature_name:
            armature = bpy.data.objects.get(armature_name)
            if armature is None or armature.type != "ARMATURE":
                self.report({"ERROR"}, f"Не найден Armature прошлого экспорта: {armature_name}")
                return {"CANCELLED"}
            try:
                armature.hide_set(False)
            except Exception:
                pass
            try:
                for obj in context.selected_objects:
                    obj.select_set(False)
                armature.select_set(True)
                context.view_layer.objects.active = armature
            except Exception as error:
                self.report({"ERROR"}, f"Не удалось выбрать Armature {armature_name}: {error}")
                return {"CANCELLED"}

            if action_name:
                action = bpy.data.actions.get(action_name)
                if action is None:
                    self.report({"ERROR"}, f"Не найден Action прошлого экспорта: {action_name}")
                    return {"CANCELLED"}
                armature.animation_data_create()
                armature.animation_data.action = action

        current_armature = get_active_armature(context)
        if current_armature is None:
            self.report({"ERROR"}, "Для повтора выберите Armature Echo")
            return {"CANCELLED"}
        if get_active_action(current_armature) is None:
            self.report({"ERROR"}, "У Armature нет Action для повторного экспорта")
            return {"CANCELLED"}

        operator_payload = dict(operator_payload)
        if "animation_behavior" not in operator_payload:
            legacy = str(operator_payload.get("main_mode", "ONCE"))
            normalized = {
                "LOOP": "LOOP_UNTIL_STOP",
                "LOOP_UNTIL_STOP": "LOOP_UNTIL_STOP",
                "HOLD": "HOLD_UNTIL_STOP",
                "HOLD_UNTIL_STOP": "HOLD_UNTIL_STOP",
            }.get(legacy, "ONCE")
            old_hold = int(operator_payload.get("end_hold_ticks", 0) or 0)
            if operator_payload.get("lifecycle_source") == "TIMELINE":
                operator_payload["animation_behavior"] = "LIFECYCLE"
            elif normalized == "LOOP_UNTIL_STOP":
                operator_payload["animation_behavior"] = "LOOP"
            elif normalized == "HOLD_UNTIL_STOP" or old_hold >= 1200:
                operator_payload["animation_behavior"] = "HOLD"
                operator_payload["end_hold_ticks"] = 0
            elif old_hold > 0:
                operator_payload["animation_behavior"] = "LIFECYCLE"
            else:
                operator_payload["animation_behavior"] = "ONE_SHOT"
            operator_payload["lifecycle_main_mode"] = normalized
        operator_payload.pop("main_mode", None)

        try:
            return bpy.ops.echo.export_animation('EXEC_DEFAULT', **operator_payload)
        except Exception as error:
            self.report({"ERROR"}, f"Не удалось повторить экспорт: {error}")
            return {"CANCELLED"}


class EchoAnimationExporterPreferences(bpy.types.AddonPreferences):
    bl_idname = ADDON_PACKAGE

    output_root: StringProperty(
        name="Папка экспорта Echo по умолчанию",
        description="Корневая папка контент-пака, внутри которой находятся animation_clips и animations.",
        subtype="DIR_PATH",
        default=DEFAULT_OUTPUT_ROOT,
    )
    resource_folder: StringProperty(
        name="Подпапка ресурсов по умолчанию",
        description="Подпапка внутри animations/ и animation_clips/. На игровой ID анимации она не влияет.",
        default=DEFAULT_RESOURCE_FOLDER,
    )
    remember_last_export_settings: BoolProperty(
        name="Запоминать последние настройки экспорта",
        description="Сохранять настройки отдельно для каждого Action и использовать последние общие параметры для новых Action",
        default=True,
    )
    last_export_settings_json: StringProperty(
        name="Последние настройки экспорта JSON",
        default="",
        options={"HIDDEN"},
    )

    def draw(self, _context):
        layout = self.layout
        box = layout.box()
        box.label(text="Настройки экспорта по умолчанию", icon="PREFERENCES")
        box.prop(self, "output_root")
        box.prop(self, "resource_folder")
        box.prop(self, "remember_last_export_settings")
        box.operator(ECHO_OT_open_export_root.bl_idname, text="Открыть папку экспорта", icon="FILE_FOLDER")
        box.label(text="Быстрый экспорт берёт путь и подпапку отсюда.", icon="INFO")
        box.label(text="Каждый Action хранит собственные настройки экспорта в .blend.", icon="RECOVER_LAST")


class ECHO_OT_quick_export_animation(bpy.types.Operator):
    bl_idname = "echo.quick_export_animation"
    bl_label = "Быстрый экспорт анимации Echo"
    bl_description = "Мгновенно экспортировать активный Action целиком с настройками по умолчанию из аддона"
    bl_options = {"REGISTER"}

    @classmethod
    def poll(cls, context):
        return get_active_armature(context) is not None

    def execute(self, context):
        armature = get_active_armature(context)
        if armature is None:
            self.report({"ERROR"}, "Выберите один Armature Echo")
            return {"CANCELLED"}

        action = get_active_action(armature)
        if action is None:
            self.report({"ERROR"}, "У выбранного Armature нет активного Action")
            return {"CANCELLED"}

        addon = context.preferences.addons.get(ADDON_PACKAGE)
        output_root = DEFAULT_OUTPUT_ROOT
        resource_folder = DEFAULT_RESOURCE_FOLDER
        if addon is not None:
            output_root = addon.preferences.output_root or DEFAULT_OUTPUT_ROOT
            resource_folder = addon.preferences.resource_folder or DEFAULT_RESOURCE_FOLDER

        payload = {
            "animation_id": sanitize_animation_name(action.name),
            "output_root": output_root,
            "resource_folder": resource_folder,
            "animation_behavior": "AUTO",
            "lifecycle_source": "AUTO",
            "lifecycle_main_mode": "ONCE",
            "range_source": "ACTION",
            "sample_step": 1,
            "optimize_holds": True,
            "main_duration_ticks": 0,
            "end_hold_ticks": 0,
            "export_scale": True,
            "enter_duration_ticks": 4,
            "exit_duration_ticks": 4,
            "interpolation": "LINEAR",
            "loop_seam": "SMOOTH",
        }

        # Reuse this Action's own settings. 3.0.x payloads are migrated here
        # too because EXEC_DEFAULT intentionally skips the dialog/invoke path.
        saved_raw = action.get(ACTION_EXPORT_SETTINGS_KEY, "")
        if saved_raw:
            try:
                saved = json.loads(saved_raw)
                if isinstance(saved, dict):
                    legacy_mode = str(saved.get("main_mode", "ONCE"))
                    normalized = {
                        "LOOP": "LOOP_UNTIL_STOP",
                        "LOOP_UNTIL_STOP": "LOOP_UNTIL_STOP",
                        "HOLD": "HOLD_UNTIL_STOP",
                        "HOLD_UNTIL_STOP": "HOLD_UNTIL_STOP",
                    }.get(legacy_mode, "ONCE")
                    if "animation_behavior" not in saved:
                        old_hold = int(saved.get("end_hold_ticks", 0) or 0)
                        if saved.get("lifecycle_source") == "TIMELINE":
                            saved["animation_behavior"] = "LIFECYCLE"
                        elif normalized == "LOOP_UNTIL_STOP":
                            saved["animation_behavior"] = "LOOP"
                        elif normalized == "HOLD_UNTIL_STOP" or old_hold >= 1200:
                            saved["animation_behavior"] = "HOLD"
                            saved["end_hold_ticks"] = 0
                        elif old_hold > 0:
                            saved["animation_behavior"] = "LIFECYCLE"
                        else:
                            saved["animation_behavior"] = "ONE_SHOT"
                    saved.setdefault("lifecycle_main_mode", normalized)
                    # Do not forward the obsolete 3.0.x enum to new logic.
                    saved.pop("main_mode", None)
                    payload.update(saved)
                    payload["output_root"] = output_root
            except Exception:
                pass

        if sanitize_animation_name(action.name) in {"pose_sit", "sit_pose"}:
            payload["animation_id"] = "pose_sit"
            payload["animation_behavior"] = "HOLD"
            payload["lifecycle_source"] = "AUTO"
            payload["lifecycle_main_mode"] = "HOLD_UNTIL_STOP"
            payload["end_hold_ticks"] = 0

        try:
            return bpy.ops.echo.export_animation('EXEC_DEFAULT', **payload)
        except Exception as error:
            self.report({"ERROR"}, f"Не удалось выполнить быстрый экспорт: {error}")
            return {"CANCELLED"}


class ECHO_OT_export_animation(bpy.types.Operator):
    bl_idname = "echo.export_animation"
    bl_label = "Экспортировать анимацию Echo"
    bl_description = "Экспортировать активный Blender Action выбранного скелета в ресурсы EchoScript. Сначала выберите Armature и нужный Action."
    bl_options = {"REGISTER"}

    animation_id: StringProperty(
        name="ID анимации",
        description="Локальное имя анимации без префикса jdh:. Например: john_thinking. Итоговый ID будет jdh:john_thinking.",
        default="my_animation",
    )
    output_root: StringProperty(
        name="Корневая папка экспорта",
        description="Корневая папка контент-пака, внутри которой находятся animation_clips и animations.",
        subtype="DIR_PATH",
        default="",
    )
    resource_folder: StringProperty(
        name="Подпапка ресурсов",
        description="Только путь хранения, например chapter_1/family. Он не становится частью игрового ID анимации.",
        default=DEFAULT_RESOURCE_FOLDER,
    )
    animation_behavior: EnumProperty(
        name="Тип анимации",
        description="Главный режим. Для обычных idle/walk/run выбирайте Цикл; для позы — Hold; фазы нужны только когда реально есть Enter/Main/Exit.",
        items=ANIMATION_BEHAVIOR_ITEMS,
        default="AUTO",
    )
    lifecycle_source: EnumProperty(
        name="Схема переходов",
        items=(
            (
                "AUTO",
                "Авто Enter / Exit",
                "Только для режима Фазы: выбранный диапазон считается Main, Enter и Exit строятся автоматически.",
            ),
            (
                "TIMELINE",
                "Фазы на Timeline",
                "Enter, Main и Exit вручную находятся в разных диапазонах одного Action.",
            ),
        ),
        default="AUTO",
    )
    range_source: EnumProperty(
        name="Диапазон кадров Main",
        items=(
            ("ACTION", "Весь Action", "Использовать полный диапазон кадров активного Action"),
            ("CURRENT", "До текущего кадра", "Использовать кадры от начала Action до текущего кадра Timeline"),
            ("SCENE", "Диапазон сцены", "Использовать Start/End из настроек текущей сцены"),
            ("CUSTOM", "Вручную", "Указать начальный и конечный кадры вручную"),
        ),
        default="ACTION",
    )
    frame_start: IntProperty(name="Начало", description="Первый экспортируемый кадр", default=1)
    frame_end: IntProperty(name="Конец", description="Последний экспортируемый кадр", default=30)
    enter_frame_start: IntProperty(name="Enter: начало", description="Начальный кадр входа в анимацию", default=0)
    enter_frame_end: IntProperty(name="Enter: конец", description="Последний кадр Enter; он должен совпадать с началом Main", default=30)
    main_frame_start: IntProperty(name="Main: начало", description="Начальный кадр основного движения", default=30)
    main_frame_end: IntProperty(name="Main: конец", description="Последний кадр Main; он должен совпадать с началом Exit", default=60)
    exit_frame_start: IntProperty(name="Exit: начало", description="Начальный кадр выхода из анимации", default=60)
    exit_frame_end: IntProperty(name="Exit: конец", description="Финальный кадр Exit, который должен вернуться к нейтральной позе", default=90)
    sample_step: IntProperty(
        name="Брать каждый N-й кадр",
        default=1,
        min=1,
        max=100,
    )
    main_duration_ticks: IntProperty(
        name="Длительность движения, тики",
        description="Длительность основного движения в игровых тиках. 0 = вычислить автоматически из FPS Blender. 20 тиков = 1 секунда.",
        default=0,
        min=0,
    )
    end_hold_ticks: IntProperty(
        name="Задержка финальной позы, тики",
        description="Сколько игровых тиков удерживать последнюю позу Main перед началом Exit. 20 тиков = 1 секунда.",
        default=0,
        min=0,
        max=72000,
    )
    export_scale: BoolProperty(
        name="Экспортировать Scale",
        description="Экспортировать масштаб костей. Отключайте только если вы точно не используете Scale в анимации.",
        default=True,
    )
    optimize_holds: BoolProperty(
        name="Убирать лишние одинаковые ключи",
        description="Сократить повторяющиеся одинаковые ключи внутри удерживаемой позы. Визуально анимация не меняется, файл становится чище.",
        default=True,
    )
    enter_duration_ticks: IntProperty(name="Enter, тики", description="Длительность автоматического входа в анимацию", default=4, min=1)
    exit_duration_ticks: IntProperty(name="Exit, тики", description="Длительность автоматического выхода из анимации", default=4, min=1)
    lifecycle_main_mode: EnumProperty(
        name="Поведение Main",
        description="Используется только в режиме Фазы (Enter → Main → Exit)",
        items=LIFECYCLE_MAIN_MODE_ITEMS,
        default="ONCE",
    )
    # Compatibility with 3.0.x saved operator payloads / Repeat Last Export.
    # New code never reads this directly; it is migrated into animation_behavior.
    main_mode: EnumProperty(
        name="Старый режим Main",
        items=(
            ("ONCE", "Один раз", "Legacy 3.0.x"),
            ("LOOP", "Цикл", "Legacy 3.0.x"),
            ("LOOP_UNTIL_STOP", "Цикл до Stop", "Современный runtime mode"),
            ("HOLD_UNTIL_STOP", "Hold до Stop", "Современный runtime mode"),
        ),
        default="ONCE",
        options={"HIDDEN"},
    )
    interpolation: EnumProperty(
        name="Интерполяция",
        items=(("STEP", "Ступенчатая", "Без плавного перехода между ключами"), ("LINEAR", "Линейная", "Равномерное движение между ключами"), ("SMOOTH", "Плавная", "Сглаженное движение"), ("CUBIC", "Кубическая", "Более мягкая кубическая интерполяция")),
        default="LINEAR",
    )
    loop_seam: EnumProperty(
        name="Стык цикла",
        items=(("HOLD", "Удержание", "Не сглаживать переход через границу цикла"), ("LINEAR", "Линейный", "Линейно соединять конец и начало цикла"), ("SMOOTH", "Плавный", "Плавно соединять конец и начало цикла")),
        default="SMOOTH",
    )
    show_quick_help: BoolProperty(name="Показывать короткую справку", default=False)
    show_paths_preview: BoolProperty(name="Показывать пути и игровые ID", default=False)
    show_sampling_options: BoolProperty(name="Показывать настройки выборки", default=False)
    show_advanced_options: BoolProperty(name="Показывать расширенные настройки", default=False)

    _timer = None
    _armature_name = ""
    _action_name = ""
    _original_frame = 1
    _frames = None
    _next_frame_index = 0
    _samples = None
    _previous_eulers = None
    _last_hold_ticks = 0
    _last_removed_keys = 0
    _neutral_pose_by_bone = None
    _loaded_saved_settings_source = ""

    def _resolved_behavior(self, armature=None) -> str:
        action = get_active_action(armature) if armature is not None else None
        source_name = action.name if action is not None else self.animation_id
        # `pose_sit` is a runtime system contract, not a generic animation.
        # Never let stale per-Action settings turn it back into One Shot/Loop.
        if sanitize_animation_name(source_name) in {"pose_sit", "sit_pose"}:
            return "HOLD"
        if self.animation_behavior != "AUTO":
            return self.animation_behavior
        return guess_animation_behavior(source_name)

    @staticmethod
    def _normalized_lifecycle_mode(value: str) -> str:
        return {
            "LOOP": "LOOP_UNTIL_STOP",
            "LOOP_UNTIL_STOP": "LOOP_UNTIL_STOP",
            "HOLD": "HOLD_UNTIL_STOP",
            "HOLD_UNTIL_STOP": "HOLD_UNTIL_STOP",
        }.get(str(value), "ONCE")

    def _migrate_legacy_payload(self, payload: dict) -> dict:
        """Upgrade 3.0.x saved settings without making animators re-enter them."""
        migrated = dict(payload)
        legacy_mode = self._normalized_lifecycle_mode(migrated.get("main_mode", "ONCE"))
        if "lifecycle_main_mode" not in migrated:
            migrated["lifecycle_main_mode"] = legacy_mode

        if "animation_behavior" not in migrated:
            lifecycle_source = migrated.get("lifecycle_source", "AUTO")
            try:
                old_hold_ticks = int(migrated.get("end_hold_ticks", 0) or 0)
            except (TypeError, ValueError):
                old_hold_ticks = 0

            if lifecycle_source == "TIMELINE":
                migrated["animation_behavior"] = "LIFECYCLE"
            elif legacy_mode == "LOOP_UNTIL_STOP":
                migrated["animation_behavior"] = "LOOP"
            elif legacy_mode == "HOLD_UNTIL_STOP" or old_hold_ticks >= 1200:
                # The old UI forced people to fake an infinite hold with huge
                # tick counts. Convert that hack to the real runtime HOLD mode.
                migrated["animation_behavior"] = "HOLD"
                migrated["end_hold_ticks"] = 0
            elif old_hold_ticks > 0:
                # A short intentional pause still belongs to a lifecycle.
                migrated["animation_behavior"] = "LIFECYCLE"
                migrated["lifecycle_main_mode"] = "ONCE"
            else:
                migrated["animation_behavior"] = "ONE_SHOT"
        return migrated

    def _apply_action_defaults(self, context, armature):
        # Reset behavior-specific values first so Blender's reused operator
        # instance cannot leak the previous Action's mode into a new one.
        self.animation_behavior = "AUTO"
        self.lifecycle_source = "AUTO"
        self.lifecycle_main_mode = "ONCE"
        self.main_mode = "ONCE"
        self.range_source = "ACTION"
        self.main_duration_ticks = 0
        self.end_hold_ticks = 0

        action = get_active_action(armature)
        if action is not None:
            self.animation_id = sanitize_animation_name(action.name)
            start, end = action.frame_range
            action_start = math.floor(start)
            action_end = max(action_start + 1, math.ceil(end))
            self.frame_start = action_start
            self.frame_end = action_end

            # These splits are used only if the animator explicitly chooses
            # the advanced lifecycle mode. Simple Loop/Hold never sees them.
            span = max(3, action_end - action_start)
            first_split = min(action_end - 2, action_start + max(1, round(span / 3)))
            first_split = max(action_start + 1, first_split)
            second_split = min(action_end - 1, action_start + max(2, round((span * 2) / 3)))
            second_split = max(first_split + 1, second_split)
            if second_split >= action_end:
                second_split = action_end - 1
            if first_split >= second_split:
                first_split = max(action_start + 1, second_split - 1)

            self.enter_frame_start = action_start
            self.enter_frame_end = first_split
            self.main_frame_start = first_split
            self.main_frame_end = second_split
            self.exit_frame_start = second_split
            self.exit_frame_end = action_end
        else:
            start = context.scene.frame_start
            end = max(start + 3, context.scene.frame_end)
            self.frame_start = start
            self.frame_end = end
            self.enter_frame_start = start
            self.enter_frame_end = start + 1
            self.main_frame_start = start + 1
            self.main_frame_end = end - 1
            self.exit_frame_start = end - 1
            self.exit_frame_end = end

    def _dialog_settings_payload(self) -> dict:
        payload = self._export_payload().copy()
        payload.pop("output_root", None)
        # Legacy compatibility field is implementation detail, not a setting.
        payload.pop("main_mode", None)
        return payload

    def _apply_dialog_payload(self, payload: dict, allowed_keys: set[str] | None = None) -> None:
        payload = self._migrate_legacy_payload(payload)
        valid_keys = set(self._dialog_settings_payload().keys())
        if allowed_keys is not None:
            valid_keys &= allowed_keys
        for key, value in payload.items():
            if key not in valid_keys or not hasattr(self, key):
                continue
            try:
                setattr(self, key, value)
            except (TypeError, ValueError, AttributeError):
                pass

    def _load_persisted_dialog_settings(self, context, action) -> str:
        addon = context.preferences.addons.get(ADDON_PACKAGE)
        if addon is None:
            return ""
        prefs = addon.preferences
        if not getattr(prefs, "remember_last_export_settings", True):
            return ""

        if action is not None:
            payload_raw = action.get(ACTION_EXPORT_SETTINGS_KEY, "")
            if payload_raw:
                try:
                    payload = json.loads(payload_raw)
                    if isinstance(payload, dict):
                        self._apply_dialog_payload(payload)
                        return "ACTION"
                except Exception:
                    pass

        payload_raw = getattr(prefs, "last_export_settings_json", "")
        if not payload_raw:
            return ""
        try:
            payload = json.loads(payload_raw)
        except Exception:
            return ""
        if not isinstance(payload, dict):
            return ""
        self._apply_dialog_payload(payload, GLOBAL_DIALOG_RESTORE_FIELDS)
        return "GLOBAL"

    def _save_persisted_dialog_settings(self, context, action) -> None:
        addon = context.preferences.addons.get(ADDON_PACKAGE)
        if addon is None:
            return
        prefs = addon.preferences
        if not getattr(prefs, "remember_last_export_settings", True):
            return

        payload = self._dialog_settings_payload()
        try:
            encoded = json.dumps(payload, ensure_ascii=False, separators=(",", ":"))
        except Exception:
            return

        # Per-Action memory survives reopening the .blend file.
        if action is not None:
            try:
                action[ACTION_EXPORT_SETTINGS_KEY] = encoded
            except Exception:
                pass

        # Global copy is only a fallback for brand-new Actions.
        try:
            prefs.last_export_settings_json = encoded
        except Exception:
            pass

    @classmethod
    def poll(cls, context):
        return get_active_armature(context) is not None

    def invoke(self, context, _event):
        armature = get_active_armature(context)
        if armature is None:
            self.report({"ERROR"}, "Выберите один Armature Echo")
            return {"CANCELLED"}

        action = get_active_action(armature)
        self._loaded_saved_settings_source = ""
        self._apply_action_defaults(context, armature)

        addon = context.preferences.addons.get(ADDON_PACKAGE)
        if addon is not None:
            self.output_root = addon.preferences.output_root
            self.resource_folder = addon.preferences.resource_folder

        self._loaded_saved_settings_source = self._load_persisted_dialog_settings(context, action)

        if not self.output_root:
            self.output_root = DEFAULT_OUTPUT_ROOT
        if not self.resource_folder:
            self.resource_folder = DEFAULT_RESOURCE_FOLDER

        return context.window_manager.invoke_props_dialog(self, width=560)

    def draw(self, context):
        layout = self.layout
        armature = get_active_armature(context)
        behavior = self._resolved_behavior(armature)

        def draw_toggle(target, prop_name: str, title: str, icon: str):
            row = layout.row(align=True)
            expanded = bool(getattr(target, prop_name))
            row.prop(
                target,
                prop_name,
                text=title,
                icon="TRIA_DOWN" if expanded else "TRIA_RIGHT",
                emboss=False,
            )
            row.label(text="", icon=icon)
            return expanded

        layout.prop(self, "animation_id")
        layout.prop(self, "resource_folder")

        mode_box = layout.box()
        mode_box.label(text="Тип анимации", icon="ACTION")
        mode_box.prop(self, "animation_behavior", expand=True)
        if self.animation_behavior == "AUTO":
            mode_box.label(text=f"Авто определило: {behavior_title(behavior)}", icon="INFO")
        if behavior == "LOOP":
            mode_box.label(text="Для idle / walk / run: только Main, без бесполезных Enter и Exit.", icon="FILE_REFRESH")
            mode_box.label(text="Action повторяется до команды Stop.")
        elif behavior == "HOLD":
            mode_box.label(text="Action проигрывается один раз и остаётся на последнем кадре до Stop.", icon="PAUSE")
            mode_box.label(text="Никаких 100000 hold-тиков — это настоящий HOLD_UNTIL_STOP.")
        elif behavior == "ONE_SHOT":
            mode_box.label(text="Только Main: проиграть один раз и завершить.", icon="PLAY")
        else:
            mode_box.label(text="Фазы нужны только когда Enter/Main/Exit реально нарисованы или нужны.", icon="SEQ_STRIP_DUPLICATE")

        tip_box = layout.box()
        tip_box.label(text="Путь экспорта берётся из настроек аддона.", icon="PREFERENCES")
        if self._loaded_saved_settings_source == "ACTION":
            tip_box.label(text="Восстановлены настройки именно этого Action.", icon="RECOVER_LAST")
        elif self._loaded_saved_settings_source == "GLOBAL":
            tip_box.label(text="Применены только безопасные общие настройки. Режим и кадры не копировались.", icon="RECOVER_LAST")

        range_box = layout.box()
        range_box.label(text="Кадры", icon="TIME")
        if behavior == "LIFECYCLE":
            range_box.prop(self, "lifecycle_source", expand=True)
            if self.lifecycle_source == "TIMELINE":
                phases = range_box.column(align=True)
                row = phases.row(align=True)
                row.label(text="Enter")
                row.prop(self, "enter_frame_start", text="Начало")
                row.prop(self, "enter_frame_end", text="Конец")
                row = phases.row(align=True)
                row.label(text="Main")
                row.prop(self, "main_frame_start", text="Начало")
                row.prop(self, "main_frame_end", text="Конец")
                row = phases.row(align=True)
                row.label(text="Exit")
                row.prop(self, "exit_frame_start", text="Начало")
                row.prop(self, "exit_frame_end", text="Конец")
                range_box.label(text="Enter конец = Main начало; Main конец = Exit начало.", icon="INFO")
            else:
                range_box.label(text="Main берётся из диапазона ниже; Enter/Exit создаются автоматически.", icon="INFO")
                range_box.prop(self, "range_source", expand=True)
        else:
            range_box.prop(self, "range_source", expand=True)

        if behavior != "LIFECYCLE" or self.lifecycle_source == "AUTO":
            if self.range_source == "CUSTOM":
                row = range_box.row(align=True)
                row.prop(self, "frame_start")
                row.prop(self, "frame_end")
            elif self.range_source == "CURRENT":
                range_box.label(text=f"Конечный кадр: {context.scene.frame_current}", icon="TIME")

        timing_box = layout.box()
        timing_box.label(text="Поведение во время игры", icon="PLAY")
        timing_box.prop(self, "main_duration_ticks")
        if self.main_duration_ticks == 0:
            timing_box.label(text="0 = длительность автоматически из FPS Blender (20 тиков = 1 секунда).", icon="INFO")

        runtime_mode = runtime_main_mode(behavior, self.lifecycle_main_mode)
        if behavior == "LOOP":
            timing_box.label(text="Режим runtime: LOOP_UNTIL_STOP — крутится сколько угодно, пока не вызван Stop.", icon="FILE_REFRESH")
        elif behavior == "HOLD":
            timing_box.label(text="Режим runtime: HOLD_UNTIL_STOP — последний кадр держится бесконечно до Stop.", icon="PAUSE")
        elif behavior == "LIFECYCLE":
            timing_box.prop(self, "lifecycle_main_mode", expand=True)
            runtime_mode = self.lifecycle_main_mode
            if runtime_mode == "ONCE":
                timing_box.prop(self, "end_hold_ticks", text="Пауза перед Exit, тики")
            else:
                timing_box.label(text="Пауза в тиках не нужна: ожидание Stop обрабатывает runtime.", icon="INFO")

            if self.lifecycle_source == "AUTO":
                row = timing_box.row(align=True)
                row.prop(self, "enter_duration_ticks")
                row.prop(self, "exit_duration_ticks")
            else:
                fps = context.scene.render.fps / context.scene.render.fps_base
                enter_ticks = max(1, round(((self.enter_frame_end - self.enter_frame_start) / fps) * 20.0))
                main_ticks = max(1, round(((self.main_frame_end - self.main_frame_start) / fps) * 20.0))
                exit_ticks = max(1, round(((self.exit_frame_end - self.exit_frame_start) / fps) * 20.0))
                timing_box.label(text=f"По Timeline: Enter {enter_ticks} т. / Main {main_ticks} т. / Exit {exit_ticks} т.", icon="TIME")

        if draw_toggle(self, "show_quick_help", "Короткая справка", "QUESTION"):
            help_box = layout.box()
            help_box.label(text="Цикл = idle/walk/run и всё, что должно повторяться до Stop.")
            help_box.label(text="Hold = дошёл до финальной позы и стоит в ней до Stop.")
            help_box.label(text="Фазы = только если нужен отдельный вход/выход.")
            help_box.label(text="20 игровых тиков = 1 секунда.")

        if draw_toggle(self, "show_paths_preview", "Пути и игровые ID", "FILE_FOLDER"):
            preview_name = sanitize_animation_name(self.animation_id)
            preview_folder = sanitize_resource_folder(self.resource_folder)
            preview_path = join_resource_path(preview_folder, preview_name)
            playable_id = runtime_animation_id(preview_name)
            command = f"/echo_animation play_animation {playable_id} jdh:molly"
            preview = layout.box()
            preview.label(text=f"Корневая папка: {bpy.path.abspath(self.output_root)}")
            preview.label(text=f"Анимация: animations/{preview_path}.animation.es")
            if behavior == "LIFECYCLE":
                preview.label(text=f"Клипы: {preview_name}_enter / _main / _exit")
            else:
                preview.label(text=f"Клип: {preview_name}_main.animation-clip.es")
            row = preview.row(align=True)
            row.label(text=f"ID: {playable_id}")
            copy_id = row.operator(ECHO_OT_copy_text.bl_idname, text="", icon="COPYDOWN")
            copy_id.text = playable_id
            row = preview.row(align=True)
            row.label(text=f"Команда: {command}")
            copy_command = row.operator(ECHO_OT_copy_text.bl_idname, text="", icon="COPYDOWN")
            copy_command.text = command

        if draw_toggle(self, "show_sampling_options", "Выборка кадров", "SEQ"):
            sample_box = layout.box()
            sample_box.prop(self, "sample_step")
            sample_box.prop(self, "optimize_holds")
            sample_box.label(text="Обычно оставляйте каждый 1-й кадр.", icon="INFO")

        if draw_toggle(self, "show_advanced_options", "Расширенные настройки", "SETTINGS"):
            advanced_box = layout.box()
            row = advanced_box.row(align=True)
            row.prop(self, "export_scale")
            row.prop(self, "interpolation")
            if runtime_mode == "LOOP_UNTIL_STOP":
                advanced_box.prop(self, "loop_seam")
            advanced_box.label(text="Scale выключайте только если уверены, что в ключах нет масштабирования.", icon="INFO")

        layout.label(text="Экспорт идёт по кадрам. Esc — отмена.", icon="INFO")

    def _resolve_frame_range(self, context, armature):
        if self.range_source == "CURRENT":
            action = get_active_action(armature)
            if action is None:
                raise RuntimeError("У выбранного Armature нет активного Action")
            start, _end = action.frame_range
            return math.floor(start), context.scene.frame_current

        if self.range_source == "SCENE":
            return context.scene.frame_start, context.scene.frame_end

        if self.range_source == "CUSTOM":
            return self.frame_start, self.frame_end

        action = get_active_action(armature)
        if action is None:
            raise RuntimeError("У выбранного Armature нет активного Action")

        start, end = action.frame_range
        return math.floor(start), math.ceil(end)

    @staticmethod
    def _sampled_frames(start: int, end: int, step: int) -> list[int]:
        frames = list(range(start, end + 1, step))
        if not frames or frames[-1] != end:
            frames.append(end)
        return frames

    def _resolve_export_plan(self, context, armature):
        behavior = self._resolved_behavior(armature)
        if behavior != "LIFECYCLE" or self.lifecycle_source == "AUTO":
            start, end = self._resolve_frame_range(context, armature)
            if end <= start:
                raise RuntimeError(
                    f"Некорректный диапазон кадров: {start}..{end}. "
                    "Конечный кадр должен быть позже начального."
                )
            frames = self._sampled_frames(start, end, self.sample_step)
            return {
                "mode": "DIRECT" if behavior != "LIFECYCLE" else "LIFECYCLE_AUTO",
                "behavior": behavior,
                "frames": frames,
                "ranges": {"main": (start, end)},
                "phase_frames": {"main": frames},
            }

        ranges = {
            "enter": (self.enter_frame_start, self.enter_frame_end),
            "main": (self.main_frame_start, self.main_frame_end),
            "exit": (self.exit_frame_start, self.exit_frame_end),
        }
        for phase, (start, end) in ranges.items():
            if end <= start:
                raise RuntimeError(
                    f"Некорректный диапазон {phase}: {start}..{end}. Конечный кадр должен быть позже начального."
                )
        if self.enter_frame_end != self.main_frame_start:
            raise RuntimeError("Конец Enter должен совпадать с началом Main.")
        if self.main_frame_end != self.exit_frame_start:
            raise RuntimeError("Конец Main должен совпадать с началом Exit.")

        phase_frames = {
            phase: self._sampled_frames(start, end, self.sample_step)
            for phase, (start, end) in ranges.items()
        }
        frames = sorted({frame for values in phase_frames.values() for frame in values})
        return {
            "mode": "LIFECYCLE_TIMELINE",
            "behavior": "LIFECYCLE",
            "frames": frames,
            "ranges": ranges,
            "phase_frames": phase_frames,
        }

    def execute(self, context):
        try:
            armature = get_active_armature(context)
            if armature is None:
                raise RuntimeError("Выберите ровно один Armature")

            schema = validate_rig(armature)
            action = get_active_action(armature)
            if action is None:
                raise RuntimeError("У выбранного Armature нет активного Action")
            self._action_name = action.name
            animation_id = sanitize_animation_name(self.animation_id)
            resource_folder = sanitize_resource_folder(self.resource_folder)
            if not animation_id:
                raise RuntimeError("ID анимации пустой")
            if not resource_folder:
                raise RuntimeError("Подпапка ресурсов не указана")

            output_root = Path(bpy.path.abspath(self.output_root))

            addon = context.preferences.addons.get(ADDON_PACKAGE)
            if addon is not None:
                addon.preferences.output_root = self.output_root
                addon.preferences.resource_folder = resource_folder
            plan = self._resolve_export_plan(context, armature)
            self._save_persisted_dialog_settings(context, action)
            frames = plan["frames"]
            start = frames[0]
            end = frames[-1]

            self.animation_id = animation_id
            self.resource_folder = resource_folder
            self._resource_folder = resource_folder
            self._rig_resource_id = schema.rig_resource_id
            self._schema = schema
            self._bone_order = schema.runtime_names
            self._base_pose_by_bone = schema_base_pose_values(
                armature,
                schema,
                self.export_scale,
            )
            self._armature_name = armature.name
            self._output_root = output_root
            self._start = start
            self._end = end
            self._export_plan = plan
            self._frames = frames
            self._next_frame_index = 0
            self._original_frame = context.scene.frame_current
            self._samples = {bone_name: [] for bone_name in self._bone_order}
            self._previous_eulers = {bone_name: None for bone_name in self._bone_order}

            wm = context.window_manager
            wm.progress_begin(0, len(frames))
            self._timer = wm.event_timer_add(0.01, window=context.window)
            wm.modal_handler_add(self)
            self.report({"INFO"}, f"Экспорт кадров: {len(frames)}…")
            return {"RUNNING_MODAL"}
        except Exception as error:
            self.report({"ERROR"}, str(error))
            return {"CANCELLED"}

    def modal(self, context, event):
        if event.type == "ESC":
            self._cleanup(context)
            self.report({"WARNING"}, "Экспорт Echo отменён")
            return {"CANCELLED"}

        if event.type != "TIMER":
            return {"PASS_THROUGH"}

        try:
            armature = bpy.data.objects.get(self._armature_name)
            if armature is None or armature.type != "ARMATURE":
                raise RuntimeError("Armature исчез во время экспорта")

            for _ in range(FRAMES_PER_TIMER_TICK):
                if self._next_frame_index >= len(self._frames):
                    paths, track_count, ticks = self._finalize(context)
                    self._cleanup(context)
                    self.report(
                        {"INFO"},
                        f"Экспортировано {self.animation_id}: треков {track_count}, тиков Main {ticks}",
                    )
                    self._remember_last_export(context, paths, ticks)
                    self._show_success_popup(context, paths, track_count, ticks)
                    return {"FINISHED"}

                frame = self._frames[self._next_frame_index]
                context.scene.frame_set(frame)
                context.view_layer.update()

                for bone_name in self._bone_order:
                    if self._schema.legacy_compatible:
                        value, compatible_euler = sample_pose_value(
                            armature,
                            bone_name,
                            self._previous_eulers[bone_name],
                            self.export_scale,
                        )
                    else:
                        value, compatible_euler = sample_schema_pose_value(
                            armature,
                            self._schema,
                            bone_name,
                            self._previous_eulers[bone_name],
                            self.export_scale,
                        )
                    self._samples[bone_name].append(value)
                    self._previous_eulers[bone_name] = compatible_euler

                self._next_frame_index += 1
                context.window_manager.progress_update(self._next_frame_index)

            return {"RUNNING_MODAL"}
        except Exception as error:
            self._cleanup(context)
            self.report({"ERROR"}, str(error))
            return {"CANCELLED"}

    def _cleanup(self, context):
        if self._timer is not None:
            context.window_manager.event_timer_remove(self._timer)
            self._timer = None
        context.window_manager.progress_end()
        context.scene.frame_set(self._original_frame)
        context.view_layer.update()

    def _phase_samples(self, bone_name: str, phase: str) -> list[PoseSample]:
        frame_index = {frame: index for index, frame in enumerate(self._frames)}
        return [
            self._samples[bone_name][frame_index[frame]]
            for frame in self._export_plan["phase_frames"][phase]
        ]

    def _authored_phase_tracks(
        self,
        exported_bones: list[str],
        phase: str,
        progress_end: float = 1.0,
        append_hold: bool = False,
    ):
        tracks = {}
        removed_keys = 0
        frames = self._export_plan["phase_frames"][phase]
        start, end = self._export_plan["ranges"][phase]

        for bone_name in exported_bones:
            values = self._phase_samples(bone_name, phase)
            track = []
            for index, (frame, value) in enumerate(zip(frames, values)):
                raw_progress = (frame - start) / (end - start)
                progress = raw_progress * progress_end
                if index < len(values) - 1:
                    interpolation = self.interpolation
                elif append_hold:
                    interpolation = "STEP"
                else:
                    interpolation = None
                track.append((progress_value(progress), value, interpolation))

            if append_hold:
                track.append(("1.0", values[-1], None))

            if self.optimize_holds:
                original_count = len(track)
                track = reduce_redundant_hold_frames(track)
                removed_keys += original_count - len(track)
            tracks[bone_name] = track

        return tracks, removed_keys

    def _capture_authored_neutral_pose(self) -> None:
        """Treat the Enter-start frame as this Action's neutral pose.

        The exporter must not assume that Echo rest pose is the active idle.
        A project may author a custom idle with raised shoulders, a body lean,
        scaled bones, or any other valid pose. Timeline lifecycle export keeps
        that exact absolute Echo pose and uses it as the return target for Exit.
        """
        frame_index = {frame: index for index, frame in enumerate(self._frames)}
        enter_start = self._export_plan["ranges"]["enter"][0]
        enter_index = frame_index[enter_start]
        self._neutral_pose_by_bone = {
            bone_name: self._samples[bone_name][enter_index]
            for bone_name in self._bone_order
        }

    def _snap_authored_exit_to_neutral(self, exported_bones: list[str]) -> None:
        """Make the exported Exit endpoint exactly equal to Enter start.

        Validation runs first. This only removes floating-point differences in
        the generated resources and never edits the Blender Action.
        """
        frame_index = {frame: index for index, frame in enumerate(self._frames)}
        exit_end = self._export_plan["ranges"]["exit"][1]
        exit_index = frame_index[exit_end]
        for bone_name in exported_bones:
            self._samples[bone_name][exit_index] = self._neutral_pose_by_bone[bone_name]

    def _validate_authored_lifecycle(self, exported_bones: list[str]) -> None:
        for bone_name in exported_bones:
            enter = self._phase_samples(bone_name, "enter")
            main = self._phase_samples(bone_name, "main")
            exit_values = self._phase_samples(bone_name, "exit")
            neutral = self._neutral_pose_by_bone[bone_name]

            checks = (
                (enter[-1], main[0], "Enter end must match Main start"),
                (main[-1], exit_values[0], "Main end must match Exit start"),
                (exit_values[-1], neutral, "Exit end must match Enter-start neutral pose"),
            )
            for actual, expected, message in checks:
                if not pose_values_close(actual, expected):
                    raise RuntimeError(
                        f"{message}: bone '{bone_name}'. "
                        f"actual {pose_debug_text(actual)}; "
                        f"expected {pose_debug_text(expected)}."
                    )

    def _finalize(self, context):
        plan_mode = self._export_plan["mode"]
        behavior = self._export_plan["behavior"]
        runtime_mode = runtime_main_mode(behavior, self.lifecycle_main_mode)
        lifecycle = plan_mode != "DIRECT"

        if plan_mode == "LIFECYCLE_TIMELINE":
            self._capture_authored_neutral_pose()

        exported_bones = [
            bone_name
            for bone_name in self._bone_order
            if any(
                differs_from_base(value, bone_name, self._base_pose_by_bone)
                for value in self._samples[bone_name]
            )
        ]
        if not exported_bones:
            raise RuntimeError(
                "Не найдено ни одной анимированной кости Echo. Проверьте активный Action, ключи и диапазон кадров."
            )

        fps = context.scene.render.fps / context.scene.render.fps_base
        # Finite hold ticks still exist as an intentional pause before Exit,
        # but ONLY for lifecycle ONCE. Infinite posing is HOLD_UNTIL_STOP now.
        hold_ticks = self.end_hold_ticks if lifecycle and runtime_mode == "ONCE" else 0
        removed_keys = 0
        enter_tracks = None
        exit_tracks = None
        enter_ticks = 0
        exit_ticks = 0

        if plan_mode == "LIFECYCLE_TIMELINE":
            self._validate_authored_lifecycle(exported_bones)
            self._snap_authored_exit_to_neutral(exported_bones)
            enter_start, enter_end = self._export_plan["ranges"]["enter"]
            main_start, main_end = self._export_plan["ranges"]["main"]
            exit_start, exit_end = self._export_plan["ranges"]["exit"]

            enter_ticks = max(1, round(((enter_end - enter_start) / fps) * 20.0))
            automatic_main_ticks = max(1, round(((main_end - main_start) / fps) * 20.0))
            exit_ticks = max(1, round(((exit_end - exit_start) / fps) * 20.0))
            motion_ticks = self.main_duration_ticks or automatic_main_ticks
            main_ticks = motion_ticks + hold_ticks
            motion_progress_end = motion_ticks / main_ticks

            enter_tracks, removed = self._authored_phase_tracks(exported_bones, "enter")
            removed_keys += removed
            main_tracks, removed = self._authored_phase_tracks(
                exported_bones,
                "main",
                progress_end=motion_progress_end,
                append_hold=hold_ticks > 0,
            )
            removed_keys += removed
            exit_tracks, removed = self._authored_phase_tracks(exported_bones, "exit")
            removed_keys += removed

        else:
            sample_count = len(self._frames)
            automatic_ticks = max(1, round(((self._end - self._start) / fps) * 20.0))
            motion_ticks = self.main_duration_ticks or automatic_ticks
            main_ticks = motion_ticks + hold_ticks
            motion_progress_end = motion_ticks / main_ticks
            main_tracks = {}

            if plan_mode == "LIFECYCLE_AUTO":
                enter_ticks = self.enter_duration_ticks
                exit_ticks = self.exit_duration_ticks
                enter_tracks = {}
                exit_tracks = {}

            for bone_name in exported_bones:
                main_frames = []
                for index, value in enumerate(self._samples[bone_name]):
                    frame = self._frames[index]
                    raw_progress = (frame - self._start) / (self._end - self._start)
                    main_progress = raw_progress * motion_progress_end
                    if index < sample_count - 1:
                        interpolation = self.interpolation
                    elif hold_ticks > 0:
                        interpolation = "STEP"
                    else:
                        interpolation = None
                    main_frames.append((progress_value(main_progress), value, interpolation))

                if hold_ticks > 0:
                    main_frames.append(("1.0", self._samples[bone_name][-1], None))

                if self.optimize_holds:
                    original_count = len(main_frames)
                    main_frames = reduce_redundant_hold_frames(main_frames)
                    removed_keys += original_count - len(main_frames)

                main_tracks[bone_name] = main_frames

                if plan_mode == "LIFECYCLE_AUTO":
                    enter_tracks[bone_name] = [
                        ("0.0", self._base_pose_by_bone[bone_name], "SMOOTH"),
                        ("1.0", self._samples[bone_name][0], None),
                    ]
                    exit_tracks[bone_name] = [
                        ("0.0", self._samples[bone_name][-1], "SMOOTH"),
                        ("1.0", self._base_pose_by_bone[bone_name], None),
                    ]

        # A loop can technically export with a non-matching seam, but surface
        # it loudly instead of making the animator discover a pop in-game.
        loop_mismatch_bones = []
        if runtime_mode == "LOOP_UNTIL_STOP":
            for bone_name in exported_bones:
                values = self._phase_samples(bone_name, "main") if plan_mode == "LIFECYCLE_TIMELINE" else self._samples[bone_name]
                if values and not pose_values_close(values[0], values[-1]):
                    loop_mismatch_bones.append(bone_name)

        base_id = runtime_animation_id(self.animation_id)
        enter_id = runtime_clip_id(self.animation_id, "enter")
        main_id = runtime_clip_id(self.animation_id, "main")
        exit_id = runtime_clip_id(self.animation_id, "exit")
        runtime_ids = (base_id, main_id) if not lifecycle else (base_id, enter_id, main_id, exit_id)
        invalid_ids = [value for value in runtime_ids if "/" in value.split(":", 1)[1]]
        if invalid_ids:
            raise RuntimeError(
                "Exporter bug: storage path leaked into runtime ID: " + ", ".join(invalid_ids)
            )

        base_class = pascal_case(self.animation_id)
        file_stem = self.animation_id
        folder_parts = self._resource_folder.split("/")
        animation_dir = self._output_root / "animations" / Path(*folder_parts)
        clip_dir = self._output_root / "animation_clips" / Path(*folder_parts) / self.animation_id
        enter_path = clip_dir / f"{file_stem}_enter.animation-clip.es"
        main_path = clip_dir / f"{file_stem}_main.animation-clip.es"
        exit_path = clip_dir / f"{file_stem}_exit.animation-clip.es"
        animation_path = animation_dir / f"{file_stem}.animation.es"

        clip_loop_seam = self.loop_seam if runtime_mode == "LOOP_UNTIL_STOP" else "HOLD"
        write_text(main_path, render_clip(
            base_class + "MainClip", main_id, main_tracks, clip_loop_seam,
            self._rig_resource_id, bone_order=self._bone_order,
        ))

        if lifecycle:
            write_text(enter_path, render_clip(
                base_class + "EnterClip", enter_id, enter_tracks, "HOLD",
                self._rig_resource_id, bone_order=self._bone_order,
            ))
            write_text(exit_path, render_clip(
                base_class + "ExitClip", exit_id, exit_tracks, "HOLD",
                self._rig_resource_id, bone_order=self._bone_order,
            ))
        else:
            # Switching an existing animation from lifecycle to Loop/Hold must
            # also clean up the old generated boilerplate, otherwise hot reload
            # leaves misleading dead files next to the real resource.
            for stale in (enter_path, exit_path):
                try:
                    if stale.exists():
                        stale.unlink()
                except OSError:
                    pass

        write_text(
            animation_path,
            render_animation(
                base_class + "Animation",
                base_id,
                main_id,
                main_ticks,
                runtime_mode,
                enter_clip_id=enter_id if lifecycle else None,
                enter_duration_ticks=enter_ticks,
                exit_clip_id=exit_id if lifecycle else None,
                exit_duration_ticks=exit_ticks,
            ),
        )

        paths = (
            enter_path if lifecycle else None,
            main_path,
            exit_path if lifecycle else None,
            animation_path,
        )

        print("\n=== ECHO EXPORT COMPLETE ===")
        print(f"Animation: {base_id}")
        print(f"Behavior: {behavior} -> runtime {runtime_mode}")
        if plan_mode == "LIFECYCLE_TIMELINE":
            print(
                "Lifecycle frames: "
                f"Enter {enter_start}..{enter_end}; Main {main_start}..{main_end}; Exit {exit_start}..{exit_end}"
            )
        else:
            print(f"Main frames: {self._start}..{self._end} ({len(self._frames)} samples)")
        if lifecycle:
            print(f"Ticks: Enter {enter_ticks}; Main {main_ticks} (motion {motion_ticks}, pause {hold_ticks}); Exit {exit_ticks}")
        else:
            print(f"Ticks: Main {main_ticks}; no Enter/Exit clips")
        if loop_mismatch_bones:
            print("WARNING: loop seam differs on: " + ", ".join(loop_mismatch_bones))
        print(f"Scale export: {'enabled' if self.export_scale else 'disabled'}")
        print(f"Removed redundant hold keys: {removed_keys}")
        print("Bones: " + ", ".join(exported_bones))
        for path in paths:
            if path is not None:
                print(path)

        self._last_hold_ticks = hold_ticks
        self._last_removed_keys = removed_keys
        self._last_phase_ticks = (enter_ticks, main_ticks, exit_ticks) if lifecycle else None
        self._last_runtime_mode = runtime_mode
        self._last_loop_mismatch_bones = tuple(loop_mismatch_bones)
        return paths, len(exported_bones), main_ticks

    def _export_payload(self) -> dict:
        return {
            "animation_id": self.animation_id,
            "output_root": str(self.output_root),
            "resource_folder": self.resource_folder,
            "animation_behavior": self.animation_behavior,
            "lifecycle_source": self.lifecycle_source,
            "lifecycle_main_mode": self.lifecycle_main_mode,
            "range_source": self.range_source,
            "frame_start": self.frame_start,
            "frame_end": self.frame_end,
            "enter_frame_start": self.enter_frame_start,
            "enter_frame_end": self.enter_frame_end,
            "main_frame_start": self.main_frame_start,
            "main_frame_end": self.main_frame_end,
            "exit_frame_start": self.exit_frame_start,
            "exit_frame_end": self.exit_frame_end,
            "sample_step": self.sample_step,
            "main_duration_ticks": self.main_duration_ticks,
            "end_hold_ticks": self.end_hold_ticks,
            "export_scale": self.export_scale,
            "optimize_holds": self.optimize_holds,
            "enter_duration_ticks": self.enter_duration_ticks,
            "exit_duration_ticks": self.exit_duration_ticks,
            "interpolation": self.interpolation,
            "loop_seam": self.loop_seam,
            "show_quick_help": self.show_quick_help,
            "show_paths_preview": self.show_paths_preview,
            "show_sampling_options": self.show_sampling_options,
            "show_advanced_options": self.show_advanced_options,
        }

    def _remember_last_export(self, context, paths, ticks):
        saved = {
            "armature_name": self._armature_name,
            "action_name": self._action_name,
            "operator": self._export_payload(),
        }
        scene = context.scene
        scene.echo_last_export_json = json.dumps(saved, ensure_ascii=False)
        mode = getattr(self, "_last_runtime_mode", "ONCE")
        mode_label = {
            "ONCE": "разово",
            "LOOP_UNTIL_STOP": "цикл",
            "HOLD_UNTIL_STOP": "hold",
        }.get(mode, mode)
        scene.echo_last_export_label = f"{self.animation_id} — {mode_label}, {ticks} тиков"
        scene.echo_last_export_path = str(paths[3])

    def _show_success_popup(self, context, paths, track_count, ticks):
        animation_id = runtime_animation_id(self.animation_id)
        command = f"/echo_animation play_animation {animation_id} jdh:molly"

        def draw(menu, _context):
            menu.layout.label(text=animation_id, icon="CHECKMARK")
            menu.layout.label(text=command, icon="CONSOLE")
            runtime_mode = getattr(self, "_last_runtime_mode", "ONCE")
            mode_label = {
                "ONCE": "Разово",
                "LOOP_UNTIL_STOP": "Цикл до Stop",
                "HOLD_UNTIL_STOP": "Hold до Stop",
            }.get(runtime_mode, runtime_mode)
            menu.layout.label(text=f"Режим: {mode_label}", icon="PLAY")
            menu.layout.label(text=f"Треков: {track_count}, Main: {ticks} тиков")
            phase_ticks = getattr(self, "_last_phase_ticks", None)
            if phase_ticks is not None:
                menu.layout.label(
                    text=f"Enter {phase_ticks[0]} / Main {phase_ticks[1]} / Exit {phase_ticks[2]} тиков",
                    icon="TIME",
                )
            if getattr(self, "_last_hold_ticks", 0) > 0:
                menu.layout.label(text=f"Задержка финальной позы: {self._last_hold_ticks} тиков", icon="PAUSE")
            mismatch = getattr(self, "_last_loop_mismatch_bones", ())
            if mismatch:
                shown = ", ".join(mismatch[:4])
                suffix = "…" if len(mismatch) > 4 else ""
                menu.layout.label(text=f"ВНИМАНИЕ: стык цикла отличается: {shown}{suffix}", icon="ERROR")
            if getattr(self, "_last_removed_keys", 0) > 0:
                menu.layout.label(text=f"Убрано лишних одинаковых ключей: {self._last_removed_keys}")
            menu.layout.label(text=str(paths[3]))

        context.window_manager.popup_menu(
            draw,
            title="Экспорт Echo завершён",
            icon="CHECKMARK",
        )


ANIMATION_CLASSES = (
    ECHO_OT_copy_text,
    ECHO_OT_open_export_root,
    ECHO_OT_open_last_export_location,
    ECHO_OT_open_addon_preferences,
    ECHO_OT_repeat_last_export,
    EchoAnimationExporterPreferences,
    ECHO_OT_quick_export_animation,
    ECHO_OT_export_animation,
)
