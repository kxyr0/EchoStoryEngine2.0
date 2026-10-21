from __future__ import annotations

import json
import math
from pathlib import Path

import bpy
from bpy.props import BoolProperty, IntProperty, StringProperty
from bpy_extras.io_utils import ImportHelper
from mathutils import Euler, Matrix, Vector

from .es_parser import ParsedAnimation, ParsedClip, ParsedFrame, ParsedPhase, parse_animation_file, parse_clip_file, resource_local_name
from .exporter import (
    ACTION_EXPORT_SETTINGS_KEY,
    BLENDER_TO_ECHO,
    ECHO_TO_BLENDER,
    REST_LOCAL_POSITION,
    UNIT,
    get_active_armature,
    rotation_only,
)
from ..rig.coordinate_converter import blender_to_echo_matrix
from ..rig.schema import resolve_skeleton_schema


ECHO_TICKS_PER_SECOND = 20.0


def _is_animation_resource(path: Path) -> bool:
    return path.name.endswith(".animation.es") and not path.name.endswith(".animation-clip.es")


def _is_clip_resource(path: Path) -> bool:
    return path.name.endswith(".animation-clip.es")


def _find_named_ancestor(path: Path, name: str) -> Path | None:
    for parent in path.parents:
        if parent.name == name:
            return parent
    return None


def _output_root_for(path: Path) -> Path | None:
    for marker in ("animations", "animation_clips"):
        ancestor = _find_named_ancestor(path, marker)
        if ancestor is not None:
            return ancestor.parent
    return None


def _resource_folder_for_animation(path: Path) -> str:
    animations = _find_named_ancestor(path, "animations")
    if animations is None:
        return ""
    try:
        relative = path.parent.relative_to(animations)
    except ValueError:
        return ""
    return relative.as_posix() if relative.parts else ""


def _clip_candidates(animation_path: Path, clip_id: str) -> list[Path]:
    root = _output_root_for(animation_path)
    local = resource_local_name(clip_id)
    if root is None:
        return []
    animations = root / "animations"
    try:
        relative_folder = animation_path.parent.relative_to(animations)
    except ValueError:
        relative_folder = Path()
    animation_name = animation_path.name.removesuffix(".animation.es")
    clip_root = root / "animation_clips" / relative_folder
    return [
        clip_root / animation_name / f"{local}.animation-clip.es",
        clip_root / f"{local}.animation-clip.es",
        clip_root / animation_name / f"{animation_name}_{local.rsplit('_', 1)[-1]}.animation-clip.es",
    ]


def _resolve_clip_path(animation_path: Path, clip_id: str) -> Path:
    for candidate in _clip_candidates(animation_path, clip_id):
        if candidate.is_file():
            return candidate
    root = _output_root_for(animation_path)
    if root is not None:
        wanted = f"{resource_local_name(clip_id)}.animation-clip.es"
        matches = list((root / "animation_clips").rglob(wanted)) if (root / "animation_clips").exists() else []
        if len(matches) == 1:
            return matches[0]
        if len(matches) > 1:
            raise RuntimeError(
                f"Найдено несколько клипов {wanted}. Импортируйте нужный .animation-clip.es напрямую."
            )
    raise RuntimeError(f"Не найден clip {clip_id} для {animation_path.name}")


def _find_animation_for_clip(clip_path: Path, clip: ParsedClip) -> tuple[Path, ParsedAnimation] | None:
    root = _output_root_for(clip_path)
    if root is None:
        return None
    local = resource_local_name(clip.resource_id)
    base = local
    for suffix in ("_main", "_enter", "_exit"):
        if base.endswith(suffix):
            base = base[: -len(suffix)]
            break

    clip_root = _find_named_ancestor(clip_path, "animation_clips")
    relative = Path()
    if clip_root is not None:
        try:
            rel_parent = clip_path.parent.relative_to(clip_root)
            parts = rel_parent.parts
            if parts and parts[-1] == base:
                parts = parts[:-1]
            relative = Path(*parts) if parts else Path()
        except ValueError:
            pass

    candidates = [root / "animations" / relative / f"{base}.animation.es"]
    for candidate in candidates:
        if candidate.is_file():
            try:
                parsed = parse_animation_file(candidate)
            except Exception:
                continue
            if any(phase.clip_id == clip.resource_id for phase in parsed.phases):
                return candidate, parsed

    animations_dir = root / "animations"
    if animations_dir.exists():
        matches = list(animations_dir.rglob(f"{base}.animation.es"))
        valid = []
        for match in matches:
            try:
                parsed = parse_animation_file(match)
            except Exception:
                continue
            if any(phase.clip_id == clip.resource_id for phase in parsed.phases):
                valid.append((match, parsed))
        if len(valid) == 1:
            return valid[0]
    return None


def _phase_ticks_from_animation(animation: ParsedAnimation, clip_id: str) -> int | None:
    for phase in animation.phases:
        if phase.clip_id == clip_id:
            return phase.duration_ticks or None
    return None


def _runtime_behavior(animation: ParsedAnimation | None, has_lifecycle: bool) -> tuple[str, str]:
    if animation is None:
        return "ONE_SHOT", "ONCE"
    mode = animation.mode.upper()
    if has_lifecycle:
        return "LIFECYCLE", mode
    if mode == "LOOP_UNTIL_STOP":
        return "LOOP", mode
    if mode == "HOLD_UNTIL_STOP":
        return "HOLD", mode
    return "ONE_SHOT", mode


def _legacy_pose_local(armature, source_name: str, runtime_name: str, frame: ParsedFrame) -> Matrix:
    pose_bone = armature.pose.bones[source_name]
    data_bone = armature.data.bones[source_name]
    if pose_bone.parent is None:
        rest_local = data_bone.matrix_local.copy()
        parent_rest_rotation = Matrix.Identity(3)
    else:
        rest_local = data_bone.parent.matrix_local.inverted_safe() @ data_bone.matrix_local
        parent_rest_rotation = rotation_only(data_bone.parent.matrix_local)

    rest_echo = Vector(REST_LOCAL_POSITION[runtime_name])
    position = Vector(frame.position or tuple(rest_echo))
    position_delta_echo = (position - rest_echo) * UNIT
    position_delta_blender = ECHO_TO_BLENDER @ position_delta_echo
    position_delta_parent = parent_rest_rotation.transposed() @ position_delta_blender
    local_location = rest_local.translation + position_delta_parent

    rotation_degrees = frame.rotation or (0.0, 0.0, 0.0)
    rotation_echo = Euler(tuple(math.radians(value) for value in rotation_degrees), "XYZ").to_matrix()
    rotation_delta_blender = ECHO_TO_BLENDER @ rotation_echo @ BLENDER_TO_ECHO
    rotation_delta_parent = (
        parent_rest_rotation.transposed()
        @ rotation_delta_blender
        @ parent_rest_rotation
    )
    rest_rotation = rotation_only(rest_local)
    local_rotation = rotation_delta_parent @ rest_rotation

    echo_scale = Vector(frame.scale or (1.0, 1.0, 1.0))
    bone_rest_rotation = rotation_only(data_bone.matrix_local)
    scale_echo_matrix = Matrix.Diagonal(echo_scale)
    scale_blender_model = ECHO_TO_BLENDER @ scale_echo_matrix @ BLENDER_TO_ECHO
    scale_local_matrix = (
        bone_rest_rotation.transposed()
        @ scale_blender_model
        @ bone_rest_rotation
    )
    local_scale_ratio = Vector(tuple(scale_local_matrix[index][index] for index in range(3)))
    rest_scale = rest_local.to_scale()
    pose_scale = Vector(tuple(rest_scale[index] * local_scale_ratio[index] for index in range(3)))

    return Matrix.LocRotScale(local_location, local_rotation.to_quaternion(), pose_scale)


def _matrix_pose_local(frame: ParsedFrame) -> Matrix:
    if frame.matrix is None or len(frame.matrix) != 16:
        raise RuntimeError("Matrix frame должен содержать 16 значений")
    echo_matrix = Matrix((
        frame.matrix[0:4],
        frame.matrix[4:8],
        frame.matrix[8:12],
        frame.matrix[12:16],
    ))
    conversion = blender_to_echo_matrix()
    return conversion.inverted_safe() @ echo_matrix @ conversion


def _pose_local_from_frame(armature, schema, runtime_name: str, source_name: str, frame: ParsedFrame) -> Matrix:
    if frame.matrix is not None:
        return _matrix_pose_local(frame)
    if not schema.legacy_compatible:
        raise RuntimeError(
            f"Track '{runtime_name}' содержит Position/Rotation/Scale, но выбранный rig не legacy-compatible. "
            "Для произвольного runtime rig ожидается Matrix(...)."
        )
    if runtime_name not in REST_LOCAL_POSITION:
        raise RuntimeError(f"Нет legacy rest pose для кости '{runtime_name}'")
    return _legacy_pose_local(armature, source_name, runtime_name, frame)


def _basis_from_pose_local(armature, source_name: str, pose_local: Matrix) -> Matrix:
    data_bone = armature.data.bones[source_name]
    if data_bone.parent is None:
        rest_local = data_bone.matrix_local.copy()
    else:
        rest_local = data_bone.parent.matrix_local.inverted_safe() @ data_bone.matrix_local
    return rest_local.inverted_safe() @ pose_local


def _key_pose_bone(pose_bone, basis: Matrix, frame_number: float) -> None:
    pose_bone.rotation_mode = "QUATERNION"
    pose_bone.matrix_basis = basis
    pose_bone.keyframe_insert(data_path="location", frame=frame_number, group=pose_bone.name)
    pose_bone.keyframe_insert(data_path="rotation_quaternion", frame=frame_number, group=pose_bone.name)
    pose_bone.keyframe_insert(data_path="scale", frame=frame_number, group=pose_bone.name)


def _blender_interpolation(echo_interpolation: str | None) -> str:
    value = (echo_interpolation or "LINEAR").upper()
    if value == "STEP":
        return "CONSTANT"
    if value == "LINEAR":
        return "LINEAR"
    return "BEZIER"


def _set_frame_interpolation(action, bone_name: str, frame_number: float, interpolation: str | None) -> None:
    desired = _blender_interpolation(interpolation)
    try:
        for fcurve in action.fcurves:
            if fcurve.group is None or fcurve.group.name != bone_name:
                continue
            for key in fcurve.keyframe_points:
                if abs(float(key.co.x) - float(frame_number)) > 1.0e-4:
                    continue
                key.interpolation = desired
                if desired == "BEZIER":
                    key.handle_left_type = "AUTO_CLAMPED"
                    key.handle_right_type = "AUTO_CLAMPED"
    except Exception:
        pass


def _new_action(armature, requested_name: str, replace_existing: bool):
    requested_name = requested_name.strip() or "echo_import"
    existing = bpy.data.actions.get(requested_name) if replace_existing else None
    create_name = requested_name
    if existing is not None:
        create_name = requested_name + "__echo_import_tmp"
        stale = bpy.data.actions.get(create_name)
        if stale is not None:
            bpy.data.actions.remove(stale)
    action = bpy.data.actions.new(name=create_name)
    action.use_fake_user = True
    armature.animation_data_create().action = action
    return action, existing, requested_name


def _duration_frames(duration_ticks: int, fps: float) -> float:
    return max(0.0, float(duration_ticks)) * fps / ECHO_TICKS_PER_SECOND


def _frame_number(parsed_frame: ParsedFrame, phase_start: float, duration_ticks: int, fps: float, clip: ParsedClip) -> float:
    if clip.legacy:
        return phase_start + parsed_frame.time * fps / ECHO_TICKS_PER_SECOND
    return phase_start + parsed_frame.time * _duration_frames(duration_ticks, fps)


def _sorted_schema_tracks(schema, clip: ParsedClip, strict_bones: bool):
    mapped = []
    unknown = []
    for track in clip.tracks:
        try:
            index = schema.index_of_runtime(track.bone_name)
        except KeyError:
            unknown.append(track.bone_name)
            continue
        entry = schema.bones[index]
        mapped.append((entry.parent_index, index, entry.runtime_name, entry.source_name, track))
    if unknown and strict_bones:
        raise RuntimeError("В выбранном rig нет костей: " + ", ".join(sorted(set(unknown))))
    mapped.sort(key=lambda item: (item[0], item[1]))
    return mapped, unknown


def _import_clip_into_action(
    context,
    armature,
    action,
    schema,
    clip: ParsedClip,
    phase_start: float,
    duration_ticks: int,
    strict_bones: bool,
    mute_constraints: bool,
):
    fps = context.scene.render.fps / max(context.scene.render.fps_base, 1.0e-8)
    mapped, unknown = _sorted_schema_tracks(schema, clip, strict_bones)
    constrained = set()
    imported_bones = set()
    key_count = 0

    for _parent_index, _index, runtime_name, source_name, track in mapped:
        pose_bone = armature.pose.bones.get(source_name)
        if pose_bone is None:
            if strict_bones:
                raise RuntimeError(f"Pose bone '{source_name}' не найден")
            unknown.append(runtime_name)
            continue
        if pose_bone.constraints:
            constrained.add(source_name)
            if mute_constraints:
                for constraint in pose_bone.constraints:
                    constraint.mute = True
        imported_bones.add(source_name)

        for parsed_frame in track.frames:
            number = _frame_number(parsed_frame, phase_start, duration_ticks, fps, clip)
            local_pose = _pose_local_from_frame(armature, schema, runtime_name, source_name, parsed_frame)
            basis = _basis_from_pose_local(armature, source_name, local_pose)
            _key_pose_bone(pose_bone, basis, number)
            _set_frame_interpolation(action, source_name, number, parsed_frame.interpolation)
            key_count += 1

    return {
        "unknown": set(unknown),
        "constrained": constrained,
        "imported_bones": imported_bones,
        "key_count": key_count,
    }


def _clip_duration_ticks(clip: ParsedClip, fallback_ticks: int) -> int:
    if clip.duration_ticks and clip.duration_ticks > 0:
        return clip.duration_ticks
    return max(1, fallback_ticks)


def _import_plan(path: Path, clip_fallback_ticks: int):
    if _is_animation_resource(path):
        animation = parse_animation_file(path)
        result = []
        for phase in animation.phases:
            clip_path = _resolve_clip_path(path, phase.clip_id)
            clip = parse_clip_file(clip_path)
            ticks = phase.duration_ticks or _clip_duration_ticks(clip, clip_fallback_ticks)
            result.append((phase, clip_path, clip, ticks))
        return animation, result, path

    if _is_clip_resource(path):
        clip = parse_clip_file(path)
        paired = _find_animation_for_clip(path, clip)
        animation = paired[1] if paired else None
        ticks = _phase_ticks_from_animation(animation, clip.resource_id) if animation else None
        ticks = ticks or _clip_duration_ticks(clip, clip_fallback_ticks)
        phase_name = "main"
        local = resource_local_name(clip.resource_id)
        for name in ("enter", "main", "exit"):
            if local.endswith("_" + name):
                phase_name = name
                break
        phase = ParsedPhase(phase_name, clip.resource_id, ticks)
        return animation, [(phase, path, clip, ticks)], paired[0] if paired else None

    raise RuntimeError("Выберите .animation.es или .animation-clip.es")


def _action_name_from(animation: ParsedAnimation | None, clip: ParsedClip | None) -> str:
    if animation is not None:
        return resource_local_name(animation.resource_id)
    if clip is not None:
        name = resource_local_name(clip.resource_id)
        for suffix in ("_main", "_enter", "_exit"):
            if name.endswith(suffix):
                name = name[: -len(suffix)]
                break
        return name
    return "echo_import"


def _store_imported_export_settings(
    action,
    source_animation_path: Path | None,
    animation: ParsedAnimation | None,
    phases,
    start_frame: float,
    fps: float,
):
    if not phases:
        return
    main_entry = next((entry for entry in phases if entry[0].name == "main"), phases[0])
    _main_phase, _main_path, main_clip, main_ticks = main_entry
    has_lifecycle = animation is not None and (animation.enter is not None or animation.exit is not None)
    behavior, lifecycle_mode = _runtime_behavior(animation, has_lifecycle)

    offsets = {}
    cursor = start_frame
    for phase, _clip_path, _clip, ticks in phases:
        phase_end = cursor + _duration_frames(ticks, fps)
        offsets[phase.name] = (int(round(cursor)), int(round(phase_end)))
        cursor = phase_end

    animation_id = resource_local_name(animation.resource_id) if animation else _action_name_from(None, main_clip)
    payload = {
        "animation_id": animation_id,
        "animation_behavior": behavior,
        "lifecycle_source": "TIMELINE" if has_lifecycle else "AUTO",
        "lifecycle_main_mode": lifecycle_mode,
        "range_source": "ACTION",
        "sample_step": 1,
        "main_duration_ticks": int(main_ticks),
        "end_hold_ticks": 0,
        "export_scale": True,
        "optimize_holds": True,
        "interpolation": next((
            (frame.interpolation or "LINEAR").upper()
            for track in main_clip.tracks
            for frame in track.frames
            if frame.interpolation
        ), "LINEAR"),
        "loop_seam": main_clip.loop_seam.upper(),
        "show_quick_help": False,
        "show_paths_preview": False,
        "show_sampling_options": False,
        "show_advanced_options": False,
    }
    if has_lifecycle:
        if "enter" in offsets:
            payload["enter_frame_start"], payload["enter_frame_end"] = offsets["enter"]
        if "main" in offsets:
            payload["main_frame_start"], payload["main_frame_end"] = offsets["main"]
        if "exit" in offsets:
            payload["exit_frame_start"], payload["exit_frame_end"] = offsets["exit"]
        if animation and animation.enter:
            payload["enter_duration_ticks"] = animation.enter.duration_ticks
        if animation and animation.exit:
            payload["exit_duration_ticks"] = animation.exit.duration_ticks

    if source_animation_path is not None:
        root = _output_root_for(source_animation_path)
        folder = _resource_folder_for_animation(source_animation_path)
        if root is not None:
            payload["output_root"] = str(root)
        if folder:
            payload["resource_folder"] = folder

    action[ACTION_EXPORT_SETTINGS_KEY] = json.dumps(payload, ensure_ascii=False, separators=(",", ":"))
    action["echo_imported_from_es"] = True
    if source_animation_path is not None:
        action["echo_import_source"] = str(source_animation_path)


class ECHO_OT_import_animation_es(bpy.types.Operator, ImportHelper):
    bl_idname = "echo.import_animation_es"
    bl_label = "Импортировать анимацию Echo (.es)"
    bl_description = "Прочитать Echo .animation.es или .animation-clip.es и создать Blender Action с ключами"
    bl_options = {"REGISTER", "UNDO"}

    filename_ext = ".es"
    filter_glob: StringProperty(default="*.es", options={"HIDDEN"})
    start_frame: IntProperty(
        name="Начальный кадр",
        description="Кадр Blender, с которого начнётся импорт",
        default=1,
    )
    clip_duration_ticks: IntProperty(
        name="Длительность одиночного клипа, тики",
        description="Используется только если импортируется новый .animation-clip.es без связанного .animation.es и Duration",
        default=20,
        min=1,
    )
    replace_existing_action: BoolProperty(
        name="Заменить Action с тем же именем",
        description="Удалить существующий Action с таким именем вместо создания Blender-копии .001",
        default=False,
    )
    set_scene_range: BoolProperty(
        name="Поставить диапазон Timeline",
        description="Установить Start/End сцены по импортированной анимации",
        default=True,
    )
    strict_bones: BoolProperty(
        name="Ошибка при неизвестной кости",
        description="Остановить импорт, если .es содержит кость, которой нет в выбранной схеме rig. Иначе такая кость пропускается",
        default=False,
    )
    mute_constraints: BoolProperty(
        name="Отключить constraints на импортируемых костях",
        description="Нужно только для runtime/deform-костей, которыми управляют constraints. ВНИМАНИЕ: меняет состояние rig; используйте осознанно",
        default=False,
    )

    @classmethod
    def poll(cls, context):
        return get_active_armature(context) is not None

    def execute(self, context):
        armature = get_active_armature(context)
        if armature is None:
            self.report({"ERROR"}, "Выберите Armature Echo перед импортом")
            return {"CANCELLED"}
        path = Path(bpy.path.abspath(self.filepath))
        if not path.is_file():
            self.report({"ERROR"}, f"Файл не найден: {path}")
            return {"CANCELLED"}

        previous_action = armature.animation_data.action if armature.animation_data else None
        action = None
        replaced_existing = None
        requested_action_name = ""
        try:
            schema = resolve_skeleton_schema(armature)
            animation, phases, paired_animation_path = _import_plan(path, self.clip_duration_ticks)
            first_clip = phases[0][2] if phases else None
            action_name = _action_name_from(animation, first_clip)
            action, replaced_existing, requested_action_name = _new_action(
                armature,
                action_name,
                self.replace_existing_action,
            )

            fps = context.scene.render.fps / max(context.scene.render.fps_base, 1.0e-8)
            cursor = float(self.start_frame)
            unknown = set()
            constrained = set()
            imported_bones = set()
            key_count = 0

            for phase, _clip_path, clip, ticks in phases:
                stats = _import_clip_into_action(
                    context,
                    armature,
                    action,
                    schema,
                    clip,
                    cursor,
                    ticks,
                    self.strict_bones,
                    self.mute_constraints,
                )
                unknown.update(stats["unknown"])
                constrained.update(stats["constrained"])
                imported_bones.update(stats["imported_bones"])
                key_count += stats["key_count"]
                cursor += _duration_frames(ticks, fps)

            if key_count == 0:
                raise RuntimeError("В .es не найдено ни одного импортируемого keyframe")

            source_animation_path = path if _is_animation_resource(path) else paired_animation_path
            _store_imported_export_settings(
                action,
                source_animation_path,
                animation,
                phases,
                float(self.start_frame),
                fps,
            )
            action["echo_import_key_count"] = key_count
            action["echo_import_bone_count"] = len(imported_bones)
            if unknown:
                action["echo_import_skipped_bones"] = ",".join(sorted(unknown))
            if constrained:
                action["echo_import_constrained_bones"] = ",".join(sorted(constrained))

            if replaced_existing is not None:
                if previous_action == replaced_existing:
                    previous_action = None
                bpy.data.actions.remove(replaced_existing)
                action.name = requested_action_name
            armature.animation_data_create().action = action
            context.scene.frame_set(self.start_frame)
            context.view_layer.update()
            if self.set_scene_range:
                context.scene.frame_start = int(math.floor(self.start_frame))
                context.scene.frame_end = max(context.scene.frame_start + 1, int(math.ceil(cursor)))

            message = f"Импортировано: {action.name} | костей {len(imported_bones)} | ключей {key_count}"
            if unknown:
                self.report({"WARNING"}, message + f" | пропущено костей: {len(unknown)}")
            elif constrained and not self.mute_constraints:
                self.report({"WARNING"}, message + f" | {len(constrained)} костей управляются constraints")
            else:
                self.report({"INFO"}, message)
            return {"FINISHED"}
        except Exception as error:
            if action is not None:
                try:
                    if armature.animation_data and armature.animation_data.action == action:
                        armature.animation_data.action = previous_action
                    if bpy.data.actions.get(action.name) is action:
                        bpy.data.actions.remove(action)
                except Exception:
                    pass
            self.report({"ERROR"}, f"Импорт Echo не удался: {error}")
            return {"CANCELLED"}


IMPORT_CLASSES = (ECHO_OT_import_animation_es,)
