from __future__ import annotations

import math
from dataclasses import dataclass

import bpy
from bpy.props import BoolProperty, EnumProperty, FloatProperty
from mathutils import Vector


UNIT = 0.0254
CONSTRAINT_PREFIX = "Echo IK"
COLLECTION_PRODUCTION = "Echo Production"
COLLECTION_HELPERS = "Echo Helpers"
COLLECTION_CONTROLS = "Echo IK Controls"
ECHO_COLLECTIONS = (
    COLLECTION_PRODUCTION,
    COLLECTION_HELPERS,
    COLLECTION_CONTROLS,
)
HELPER_BONES = ("root", "left_shoulder", "right_shoulder")
PRODUCTION_BONES = (
    "body",
    "head",
    "left_upper_arm",
    "left_forearm",
    "right_upper_arm",
    "right_forearm",
    "left_thigh",
    "left_shin",
    "right_thigh",
    "right_shin",
)
REQUIRED_BONES = HELPER_BONES + PRODUCTION_BONES
CONTROL_LENGTH = UNIT * 2.0
DEFAULT_POLE_DISTANCE = UNIT * 8.0

COLOR_PRODUCTION = (
    (0.035, 0.18, 0.75),
    (0.12, 0.48, 1.0),
    (0.55, 0.78, 1.0),
)
COLOR_HELPER = (
    (0.18, 0.18, 0.18),
    (0.48, 0.48, 0.48),
    (0.82, 0.82, 0.82),
)
COLOR_CONTROL = (
    (0.035, 0.55, 0.10),
    (0.14, 0.95, 0.26),
    (0.58, 1.0, 0.64),
)


@dataclass(frozen=True)
class ChainSpec:
    key: str
    upper: str
    lower: str
    target: str
    pole: str
    limb: str

    @property
    def constraint_name(self) -> str:
        return f"{CONSTRAINT_PREFIX} {self.key}"


CHAINS = (
    ChainSpec(
        "left_arm",
        "left_upper_arm",
        "left_forearm",
        "left_hand_ik",
        "left_elbow_pole",
        "ARM",
    ),
    ChainSpec(
        "right_arm",
        "right_upper_arm",
        "right_forearm",
        "right_hand_ik",
        "right_elbow_pole",
        "ARM",
    ),
    ChainSpec(
        "left_leg",
        "left_thigh",
        "left_shin",
        "left_foot_ik",
        "left_knee_pole",
        "LEG",
    ),
    ChainSpec(
        "right_leg",
        "right_thigh",
        "right_shin",
        "right_foot_ik",
        "right_knee_pole",
        "LEG",
    ),
)
CONTROL_BONES = tuple(
    name
    for chain in CHAINS
    for name in (chain.target, chain.pole)
)


def active_armature(context: bpy.types.Context) -> bpy.types.Object | None:
    obj = context.object
    if obj is not None and obj.type == "ARMATURE":
        return obj

    selected = [obj for obj in context.selected_objects if obj.type == "ARMATURE"]
    return selected[0] if len(selected) == 1 else None


def validate_armature(armature: bpy.types.Object) -> None:
    missing = [
        name for name in REQUIRED_BONES
        if armature.data.bones.get(name) is None
    ]
    if missing:
        raise RuntimeError("В скелете не хватает обязательных костей Echo: " + ", ".join(missing))


def active_chains(scene: bpy.types.Scene) -> tuple[ChainSpec, ...]:
    if getattr(scene, "echo_ik_include_legs", True):
        return CHAINS
    return tuple(chain for chain in CHAINS if chain.limb == "ARM")


def forward_vector(scene: bpy.types.Scene) -> Vector:
    axis = getattr(scene, "echo_ik_forward_axis", "NEG_Y")
    return Vector((0.0, 1.0 if axis == "POS_Y" else -1.0, 0.0))


def activate(
    context: bpy.types.Context,
    armature: bpy.types.Object,
    mode: str,
) -> None:
    current = context.view_layer.objects.active
    if current is not None and current.mode != "OBJECT":
        bpy.ops.object.mode_set(mode="OBJECT")

    bpy.ops.object.select_all(action="DESELECT")
    armature.hide_set(False)
    armature.hide_viewport = False
    armature.select_set(True)
    context.view_layer.objects.active = armature

    if mode != "OBJECT":
        bpy.ops.object.mode_set(mode=mode)


def constraint_for(armature: bpy.types.Object, chain: ChainSpec):
    lower = armature.pose.bones.get(chain.lower)
    if lower is None:
        return None
    return lower.constraints.get(chain.constraint_name)


def all_constraints(armature: bpy.types.Object):
    result = []
    for chain in CHAINS:
        constraint = constraint_for(armature, chain)
        if constraint is not None:
            result.append(constraint)
    return result


def setup_count(armature: bpy.types.Object) -> int:
    return sum(
        1
        for chain in CHAINS
        if armature.data.bones.get(chain.target) is not None
        and armature.data.bones.get(chain.pole) is not None
        and constraint_for(armature, chain) is not None
    )


def projected_direction(direction: Vector, axis: Vector) -> Vector:
    if axis.length_squared <= 1e-12:
        return direction.normalized()

    axis = axis.normalized()
    projected = direction - axis * direction.dot(axis)
    if projected.length_squared <= 1e-12:
        fallback = Vector((1.0, 0.0, 0.0))
        projected = fallback - axis * fallback.dot(axis)
    if projected.length_squared <= 1e-12:
        projected = Vector((0.0, 0.0, 1.0))
    return projected.normalized()


def automatic_pole_position(
    start: Vector,
    joint: Vector,
    end: Vector,
    forward: Vector,
    distance: float,
) -> Vector:
    direction = projected_direction(forward, end - start)
    return joint + direction * distance


def signed_angle(first: Vector, second: Vector, normal: Vector) -> float:
    if (
        first.length_squared <= 1e-12
        or second.length_squared <= 1e-12
        or normal.length_squared <= 1e-12
    ):
        return 0.0

    first = first.normalized()
    second = second.normalized()
    normal = normal.normalized()
    sine = normal.dot(first.cross(second))
    cosine = max(-1.0, min(1.0, first.dot(second)))
    return math.atan2(sine, cosine)


def calculate_pole_angle(
    armature: bpy.types.Object,
    chain: ChainSpec,
) -> float:
    base = armature.data.bones[chain.upper]
    lower = armature.data.bones[chain.lower]
    pole = armature.data.bones[chain.pole]

    ik_axis = lower.tail_local - base.head_local
    pole_normal = ik_axis.cross(pole.head_local - base.head_local)
    projected_axis = pole_normal.cross(base.vector)
    # Blender's IK pole-angle convention is the negative signed angle
    # from the base bone X axis to the projected pole axis.
    return -signed_angle(base.x_axis, projected_axis, base.vector)


def set_pose_position(pose_bone: bpy.types.PoseBone, position: Vector) -> None:
    matrix = pose_bone.matrix.copy()
    matrix.translation = position
    pose_bone.matrix = matrix


def capture_positions(
    armature: bpy.types.Object,
    chains: tuple[ChainSpec, ...],
    forward: Vector,
    distance: float,
) -> dict[str, tuple[Vector, Vector]]:
    result = {}
    for chain in chains:
        upper = armature.pose.bones[chain.upper]
        lower = armature.pose.bones[chain.lower]
        result[chain.key] = (
            lower.tail.copy(),
            automatic_pole_position(
                upper.head.copy(),
                lower.head.copy(),
                lower.tail.copy(),
                forward,
                distance,
            ),
        )
    return result


def create_missing_controls(
    context: bpy.types.Context,
    armature: bpy.types.Object,
    chains: tuple[ChainSpec, ...],
    forward: Vector,
    distance: float,
) -> set[str]:
    rest_layout = {}
    for chain in chains:
        upper = armature.data.bones[chain.upper]
        lower = armature.data.bones[chain.lower]
        side = 1.0 if chain.key.startswith("left_") else -1.0

        target_head = lower.tail_local.copy()
        target_tail = target_head + Vector((side * CONTROL_LENGTH, 0.0, 0.0))
        pole_head = automatic_pole_position(
            upper.head_local.copy(),
            lower.head_local.copy(),
            lower.tail_local.copy(),
            forward,
            distance,
        )
        pole_tail = pole_head + Vector((0.0, 0.0, CONTROL_LENGTH))
        rest_layout[chain.key] = (
            target_head,
            target_tail,
            pole_head,
            pole_tail,
        )

    activate(context, armature, "EDIT")
    root = armature.data.edit_bones.get("root")
    created = set()

    for chain in chains:
        target_head, target_tail, pole_head, pole_tail = rest_layout[chain.key]

        if armature.data.edit_bones.get(chain.target) is None:
            target = armature.data.edit_bones.new(chain.target)
            target.head = target_head
            target.tail = target_tail
            target.parent = root
            target.use_connect = False
            target.use_deform = False
            created.add(chain.target)

        if armature.data.edit_bones.get(chain.pole) is None:
            pole = armature.data.edit_bones.new(chain.pole)
            pole.head = pole_head
            pole.tail = pole_tail
            pole.parent = root
            pole.use_connect = False
            pole.use_deform = False
            created.add(chain.pole)

    bpy.ops.object.mode_set(mode="POSE")
    return created


def place_new_controls(
    armature: bpy.types.Object,
    chains: tuple[ChainSpec, ...],
    created: set[str],
    captured: dict[str, tuple[Vector, Vector]],
) -> None:
    for chain in chains:
        target_position, pole_position = captured[chain.key]
        if chain.target in created:
            set_pose_position(
                armature.pose.bones[chain.target],
                target_position,
            )
        if chain.pole in created:
            set_pose_position(
                armature.pose.bones[chain.pole],
                pole_position,
            )


def configure_control_locks(
    armature: bpy.types.Object,
    chains: tuple[ChainSpec, ...],
) -> None:
    for chain in chains:
        target = armature.pose.bones.get(chain.target)
        pole = armature.pose.bones.get(chain.pole)

        if target is not None:
            target.rotation_mode = "XYZ"
            target.lock_scale = (True, True, True)

        if pole is not None:
            pole.rotation_mode = "XYZ"
            pole.lock_rotation = (True, True, True)
            pole.lock_scale = (True, True, True)


def ensure_constraints(
    armature: bpy.types.Object,
    chains: tuple[ChainSpec, ...],
) -> int:
    created = 0
    for chain in chains:
        upper = armature.pose.bones[chain.upper]
        lower = armature.pose.bones[chain.lower]
        constraint = lower.constraints.get(chain.constraint_name)
        is_new = constraint is None

        if constraint is None:
            constraint = lower.constraints.new("IK")
            constraint.name = chain.constraint_name
            created += 1

        constraint.target = armature
        constraint.subtarget = chain.target
        constraint.pole_target = armature
        constraint.pole_subtarget = chain.pole
        constraint.chain_count = 2
        constraint.pole_angle = calculate_pole_angle(armature, chain)

        if hasattr(constraint, "head_tail"):
            constraint.head_tail = 0.0
        if hasattr(constraint, "iterations"):
            constraint.iterations = 64
        if hasattr(constraint, "use_tail"):
            constraint.use_tail = True
        if hasattr(constraint, "use_stretch"):
            constraint.use_stretch = False
        if hasattr(constraint, "use_rotation"):
            constraint.use_rotation = False

        upper.ik_stretch = 0.0
        lower.ik_stretch = 0.0
        if is_new:
            constraint.influence = 1.0
            constraint.mute = False

    return created


def ensure_collection(armature: bpy.types.Object, name: str):
    collections = getattr(armature.data, "collections", None)
    if collections is None:
        return None

    collection = collections.get(name)
    if collection is None:
        collection = collections.new(name)
    if hasattr(collection, "is_visible"):
        collection.is_visible = True
    return collection


def assign_collection(
    armature: bpy.types.Object,
    bone_name: str,
    collection_name: str,
) -> None:
    bone = armature.data.bones.get(bone_name)
    collections = getattr(armature.data, "collections", None)
    if bone is None or collections is None:
        return

    target = ensure_collection(armature, collection_name)
    if target is None:
        return

    for name in ECHO_COLLECTIONS:
        collection = collections.get(name)
        if collection is None or not hasattr(collection, "unassign"):
            continue
        try:
            collection.unassign(bone)
        except (RuntimeError, TypeError):
            pass

    if hasattr(target, "assign"):
        try:
            target.assign(bone)
        except (RuntimeError, TypeError):
            pass


def set_custom_color(owner, palette) -> None:
    color = getattr(owner, "color", None)
    if color is None:
        return

    try:
        normal, select, active = palette
        color.palette = "CUSTOM"
        color.custom.normal = normal
        color.custom.select = select
        color.custom.active = active
    except (AttributeError, TypeError, ValueError):
        pass


def color_bone(
    armature: bpy.types.Object,
    bone_name: str,
    collection_name: str,
    palette,
) -> None:
    data_bone = armature.data.bones.get(bone_name)
    pose_bone = armature.pose.bones.get(bone_name)
    if data_bone is None:
        return

    assign_collection(armature, bone_name, collection_name)
    set_custom_color(data_bone, palette)
    if pose_bone is not None:
        set_custom_color(pose_bone, palette)


def apply_visuals(armature: bpy.types.Object) -> None:
    armature.show_in_front = True
    armature.data.show_names = True
    if hasattr(armature.data, "display_type"):
        armature.data.display_type = "OCTAHEDRAL"

    for name in PRODUCTION_BONES:
        color_bone(
            armature,
            name,
            COLLECTION_PRODUCTION,
            COLOR_PRODUCTION,
        )

    for name in HELPER_BONES:
        color_bone(
            armature,
            name,
            COLLECTION_HELPERS,
            COLOR_HELPER,
        )

    for name in CONTROL_BONES:
        bone = armature.data.bones.get(name)
        if bone is None:
            continue
        bone.use_deform = False
        bone.hide_select = False
        color_bone(
            armature,
            name,
            COLLECTION_CONTROLS,
            COLOR_CONTROL,
        )


def mute_constraints(armature: bpy.types.Object, muted: bool) -> None:
    for constraint in all_constraints(armature):
        constraint.mute = muted


class ECHO_OT_setup_ik_rig(bpy.types.Operator):
    bl_idname = "echo.setup_ik_rig"
    bl_label = "Настроить / починить IK"
    bl_description = (
        "Создать или восстановить IK-контролы кистей/стоп, pole-контролы локтей/коленей и IK-ограничения. Используйте один раз перед IK-анимацией."
    )
    bl_options = {"REGISTER", "UNDO"}

    @classmethod
    def poll(cls, context):
        return active_armature(context) is not None

    def execute(self, context):
        try:
            armature = active_armature(context)
            if armature is None:
                raise RuntimeError("Выберите Armature Echo")
            validate_armature(armature)

            chains = active_chains(context.scene)
            forward = forward_vector(context.scene)
            distance = context.scene.echo_ik_pole_distance
            activate(context, armature, "POSE")
            context.view_layer.update()

            captured = capture_positions(
                armature,
                chains,
                forward,
                distance,
            )
            created_bones = create_missing_controls(
                context,
                armature,
                chains,
                forward,
                distance,
            )
            configure_control_locks(armature, chains)
            place_new_controls(
                armature,
                chains,
                created_bones,
                captured,
            )
            context.view_layer.update()
            created_constraints = ensure_constraints(armature, chains)
            apply_visuals(armature)
            context.view_layer.update()

            self.report(
                {"INFO"},
                f"IK готов: цепей {len(chains)}, создано контролов {len(created_bones)}, ограничений {created_constraints}",
            )
            return {"FINISHED"}
        except Exception as error:
            self.report({"ERROR"}, str(error))
            return {"CANCELLED"}


class ECHO_OT_auto_pole(bpy.types.Operator):
    bl_idname = "echo.auto_pole"
    bl_label = "Расставить локти/колени"
    bl_description = (
        "Автоматически поставить pole-контролы перед локтями и коленями. Нажмите, если суставы сгибаются не туда или контролы потерялись."
    )
    bl_options = {"REGISTER", "UNDO"}

    @classmethod
    def poll(cls, context):
        armature = active_armature(context)
        return armature is not None and setup_count(armature) > 0

    def execute(self, context):
        armature = active_armature(context)
        chains = active_chains(context.scene)
        forward = forward_vector(context.scene)
        distance = context.scene.echo_ik_pole_distance
        activate(context, armature, "POSE")
        context.view_layer.update()

        moved = 0
        for chain in chains:
            pole = armature.pose.bones.get(chain.pole)
            if pole is None:
                continue

            upper = armature.pose.bones[chain.upper]
            lower = armature.pose.bones[chain.lower]
            set_pose_position(
                pole,
                automatic_pole_position(
                    upper.head.copy(),
                    lower.head.copy(),
                    lower.tail.copy(),
                    forward,
                    distance,
                ),
            )
            moved += 1

        context.view_layer.update()
        self.report({"INFO"}, f"Pole-контролы расставлены: {moved}")
        return {"FINISHED"}


class ECHO_OT_snap_ik_to_fk(bpy.types.Operator):
    bl_idname = "echo.snap_ik_to_fk"
    bl_label = "IK по текущей позе"
    bl_description = (
        "Переместить зелёные IK-контролы в текущую позу рук/ног, чтобы включение IK не дёрнуло конечности."
    )
    bl_options = {"REGISTER", "UNDO"}

    @classmethod
    def poll(cls, context):
        armature = active_armature(context)
        return armature is not None and setup_count(armature) > 0

    def execute(self, context):
        armature = active_armature(context)
        chains = active_chains(context.scene)
        constraints = all_constraints(armature)
        previous = {constraint.name: constraint.mute for constraint in constraints}

        activate(context, armature, "POSE")
        for constraint in constraints:
            constraint.mute = True
        context.view_layer.update()

        captured = capture_positions(
            armature,
            chains,
            forward_vector(context.scene),
            context.scene.echo_ik_pole_distance,
        )
        for chain in chains:
            target = armature.pose.bones.get(chain.target)
            pole = armature.pose.bones.get(chain.pole)
            if target is None or pole is None:
                continue
            target_position, pole_position = captured[chain.key]
            set_pose_position(target, target_position)
            set_pose_position(pole, pole_position)

        for constraint in constraints:
            constraint.mute = previous[constraint.name]
        context.view_layer.update()
        self.report({"INFO"}, "IK-контролы совмещены с текущей FK-позой")
        return {"FINISHED"}


class ECHO_OT_toggle_ik(bpy.types.Operator):
    bl_idname = "echo.toggle_ik"
    bl_label = "Вкл./выкл. IK"
    bl_description = "Временно выключить или включить все IK-ограничения Echo. Ключи анимации не удаляются."
    bl_options = {"REGISTER", "UNDO"}

    @classmethod
    def poll(cls, context):
        armature = active_armature(context)
        return armature is not None and setup_count(armature) > 0

    def execute(self, context):
        armature = active_armature(context)
        constraints = all_constraints(armature)
        should_mute = any(not constraint.mute for constraint in constraints)
        mute_constraints(armature, should_mute)
        context.view_layer.update()
        state = "выключен" if should_mute else "включен"
        self.report({"INFO"}, f"Echo IK {state}")
        return {"FINISHED"}


class ECHO_OT_flip_poles(bpy.types.Operator):
    bl_idname = "echo.flip_poles"
    bl_label = "Развернуть сгиб суставов"
    bl_description = "Развернуть направление сгиба всех IK-суставов на 180°. Используйте, если локти или колени выворачиваются в противоположную сторону."
    bl_options = {"REGISTER", "UNDO"}

    @classmethod
    def poll(cls, context):
        armature = active_armature(context)
        return armature is not None and setup_count(armature) > 0

    def execute(self, context):
        armature = active_armature(context)
        flipped = 0
        for constraint in all_constraints(armature):
            constraint.pole_angle = math.remainder(
                constraint.pole_angle + math.pi,
                math.tau,
            )
            flipped += 1
        context.view_layer.update()
        self.report({"INFO"}, f"Развёрнуты направления сгиба: {flipped}")
        return {"FINISHED"}


class ECHO_OT_select_ik_controls(bpy.types.Operator):
    bl_idname = "echo.select_ik_controls"
    bl_label = "Выбрать IK-контролы"
    bl_description = "Выбрать все зелёные контролы кистей/стоп и pole-контролы локтей/коленей для удобной анимации."
    bl_options = {"REGISTER", "UNDO"}

    @classmethod
    def poll(cls, context):
        armature = active_armature(context)
        return armature is not None and setup_count(armature) > 0

    def execute(self, context):
        armature = active_armature(context)
        activate(context, armature, "POSE")

        for bone in armature.data.bones:
            bone.select = False

        selected = []
        for name in CONTROL_BONES:
            bone = armature.data.bones.get(name)
            if bone is None:
                continue
            bone.hide = False
            bone.select = True
            selected.append(bone)

        if selected:
            armature.data.bones.active = selected[0]
        self.report({"INFO"}, f"Выбрано IK-контролов: {len(selected)}")
        return {"FINISHED"}


class ECHO_OT_apply_rig_colors(bpy.types.Operator):
    bl_idname = "echo.apply_rig_colors"
    bl_label = "Раскрасить кости"
    bl_description = (
        "Вернуть понятные цвета: зелёные — IK-контролы, синие — рабочие кости персонажа, серые — служебные."
    )
    bl_options = {"REGISTER", "UNDO"}

    @classmethod
    def poll(cls, context):
        return active_armature(context) is not None

    def execute(self, context):
        armature = active_armature(context)
        activate(context, armature, "POSE")
        apply_visuals(armature)
        self.report({"INFO"}, "Цвета и коллекции костей Echo восстановлены")
        return {"FINISHED"}


class ECHO_OT_remove_ik_rig(bpy.types.Operator):
    bl_idname = "echo.remove_ik_rig"
    bl_label = "Удалить IK-настройку"
    bl_description = (
        "Удалить созданные Echo IK-ограничения и зелёные контролы. Рабочие кости и анимация модели останутся."
    )
    bl_options = {"REGISTER", "UNDO"}

    @classmethod
    def poll(cls, context):
        armature = active_armature(context)
        return armature is not None and setup_count(armature) > 0

    def invoke(self, context, event):
        return context.window_manager.invoke_confirm(self, event)

    def execute(self, context):
        armature = active_armature(context)
        activate(context, armature, "POSE")

        removed_constraints = 0
        for chain in CHAINS:
            lower = armature.pose.bones.get(chain.lower)
            if lower is None:
                continue
            constraint = lower.constraints.get(chain.constraint_name)
            if constraint is not None:
                lower.constraints.remove(constraint)
                removed_constraints += 1

        bpy.ops.object.mode_set(mode="EDIT")
        removed_bones = 0
        for name in CONTROL_BONES:
            bone = armature.data.edit_bones.get(name)
            if bone is None:
                continue
            armature.data.edit_bones.remove(bone)
            removed_bones += 1

        bpy.ops.object.mode_set(mode="POSE")
        apply_visuals(armature)
        context.view_layer.update()
        self.report(
            {"INFO"},
            f"Удалено IK-ограничений: {removed_constraints}, контролов: {removed_bones}",
        )
        return {"FINISHED"}


def register_properties() -> None:
    if not hasattr(bpy.types.Scene, "echo_ik_include_legs"):
        bpy.types.Scene.echo_ik_include_legs = BoolProperty(
            name="Добавить IK ног",
            description="При настройке IK создать также контролы стоп и коленей. Выключите, если нужны только руки.",
            default=True,
        )

    if not hasattr(bpy.types.Scene, "echo_ik_forward_axis"):
        bpy.types.Scene.echo_ik_forward_axis = EnumProperty(
            name="Куда смотрит персонаж",
            description="Направление лица персонажа в локальных координатах скелета. Нужно для правильной автоматической расстановки локтей и коленей.",
            items=(
                (
                    "NEG_Y",
                    "Вперёд (-Y)",
                    "Стандартный вариант Echo: персонаж смотрит в сторону Blender -Y",
                ),
                (
                    "POS_Y",
                    "Вперёд (+Y)",
                    "Выберите, только если импортированная модель реально смотрит в сторону Blender +Y",
                ),
            ),
            default="NEG_Y",
        )

    if not hasattr(bpy.types.Scene, "echo_ik_pole_distance"):
        bpy.types.Scene.echo_ik_pole_distance = FloatProperty(
            name="Отступ локтя/колена",
            description="Расстояние от локтя/колена до зелёного pole-контрола. Чем больше значение, тем дальше будет управляющий квадрат.",
            default=DEFAULT_POLE_DISTANCE,
            min=UNIT,
            max=10.0,
            subtype="DISTANCE",
        )


def unregister_properties() -> None:
    for name in (
        "echo_ik_include_legs",
        "echo_ik_forward_axis",
        "echo_ik_pole_distance",
    ):
        if hasattr(bpy.types.Scene, name):
            delattr(bpy.types.Scene, name)


def draw_ik_panel(layout, context) -> None:
    armature = active_armature(context)
    scene = context.scene
    box = layout.box()
    row = box.row(align=True)
    expanded = bool(scene.echo_ui_show_ik_block)
    row.prop(
        scene,
        "echo_ui_show_ik_block",
        text="IK для аниматора",
        icon="TRIA_DOWN" if expanded else "TRIA_RIGHT",
        emboss=False,
    )
    row.label(text="", icon="CONSTRAINT_BONE")

    if not expanded:
        return

    if armature is None:
        box.label(text="Выберите Armature Echo")
        return

    count = setup_count(armature)
    box.label(
        text=f"Готово IK-цепей: {count}/4",
        icon="CHECKMARK" if count else "INFO",
    )
    box.prop(context.scene, "echo_ik_include_legs")
    box.prop(context.scene, "echo_ik_forward_axis")
    box.prop(context.scene, "echo_ik_pole_distance")

    row = box.row(align=True)
    row.operator(ECHO_OT_setup_ik_rig.bl_idname, text="Настроить / починить IK", icon="CONSTRAINT_BONE")
    row.operator(ECHO_OT_remove_ik_rig.bl_idname, text="", icon="TRASH")

    row = box.row(align=True)
    row.operator(ECHO_OT_auto_pole.bl_idname, text="Локти / колени")
    row.operator(ECHO_OT_snap_ik_to_fk.bl_idname, text="IK по позе")

    row = box.row(align=True)
    row.operator(ECHO_OT_select_ik_controls.bl_idname, text="Выбрать контролы")
    row.operator(ECHO_OT_toggle_ik.bl_idname, text="Вкл./выкл. IK")

    row = box.row(align=True)
    row.operator(ECHO_OT_apply_rig_colors.bl_idname, text="Цвета костей")
    row.operator(ECHO_OT_flip_poles.bl_idname, text="Развернуть сгиб")

    help_row = box.row(align=True)
    help_expanded = bool(scene.echo_ui_show_ik_help)
    help_row.prop(
        scene,
        "echo_ui_show_ik_help",
        text="Подсказки по IK",
        icon="TRIA_DOWN" if help_expanded else "TRIA_RIGHT",
        emboss=False,
    )
    help_row.label(text="", icon="INFO")

    if help_expanded:
        help_box = box.box()
        help_box.label(text="Зелёные — контролы. Синие — рабочие кости.")
        help_box.label(text="Серые — служебные кости.")
        help_box.label(text="Двигайте зелёные контролы кистей и стоп.", icon="INFO")
        help_box.label(text="Pole-контролы задают сгиб локтей и коленей.")
        help_box.label(text="IK запекается в экспорт; зелёные контролы не попадут в игру.")


IK_CLASSES = (
    ECHO_OT_setup_ik_rig,
    ECHO_OT_auto_pole,
    ECHO_OT_snap_ik_to_fk,
    ECHO_OT_toggle_ik,
    ECHO_OT_flip_poles,
    ECHO_OT_select_ik_controls,
    ECHO_OT_apply_rig_colors,
    ECHO_OT_remove_ik_rig,
)
