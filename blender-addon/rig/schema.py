from __future__ import annotations

import re
from dataclasses import dataclass
from typing import Any, Iterable

from .therig import (
    THERIG_EXPLICIT_RUNTIME_BONES,
    THERIG_RIG_ID,
    THERIG_SCHEMA_ID,
    is_therig_2_6,
    therig_runtime_name,
)


LEGACY_RIG_ID = "jdh:humanoid"
LEGACY_V1_SCHEMA_ID = "JDH_HUMANOID_V1"
LEGACY_SCHEMA_ID = "JDH_HUMANOID_V2"
AUTO_SCHEMA_ID = "AUTO"
MAX_RUNTIME_BONES = 512

LEGACY_V1_BONE_ORDER = (
    "root",
    "body",
    "head",
    "left_shoulder",
    "left_upper_arm",
    "left_forearm",
    "right_shoulder",
    "right_upper_arm",
    "right_forearm",
    "left_thigh",
    "left_shin",
    "right_thigh",
    "right_shin",
)

LEGACY_BONE_ORDER = (
    "root",
    "body_lower",
    "body",
    "head",
    "left_shoulder",
    "left_upper_arm",
    "left_forearm",
    "right_shoulder",
    "right_upper_arm",
    "right_forearm",
    "left_thigh",
    "left_shin",
    "right_thigh",
    "right_shin",
)

LEGACY_V1_PARENT = {
    "root": None,
    "body": "root",
    "head": "body",
    "left_shoulder": "body",
    "left_upper_arm": "left_shoulder",
    "left_forearm": "left_upper_arm",
    "right_shoulder": "body",
    "right_upper_arm": "right_shoulder",
    "right_forearm": "right_upper_arm",
    "left_thigh": "root",
    "left_shin": "left_thigh",
    "right_thigh": "root",
    "right_shin": "right_thigh",
}

LEGACY_PARENT = {
    "root": None,
    "body_lower": "root",
    "body": "body_lower",
    "head": "body",
    "left_shoulder": "body",
    "left_upper_arm": "left_shoulder",
    "left_forearm": "left_upper_arm",
    "right_shoulder": "body",
    "right_upper_arm": "right_shoulder",
    "right_forearm": "right_upper_arm",
    "left_thigh": "root",
    "left_shin": "left_thigh",
    "right_thigh": "root",
    "right_shin": "right_thigh",
}

LEGACY_REST_LOCAL_POSITION = {
    "root": (0.0, 0.0, 0.0),
    "body_lower": (0.0, 0.0, 0.0),
    "body": (0.0, 12.0, 0.0),
    "head": (0.0, 12.0, 0.0),
    "left_shoulder": (4.0, 10.0, 0.0),
    "left_upper_arm": (0.0, 0.0, 0.0),
    "left_forearm": (0.0, -6.0, 0.0),
    "right_shoulder": (-4.0, 10.0, 0.0),
    "right_upper_arm": (0.0, 0.0, 0.0),
    "right_forearm": (0.0, -6.0, 0.0),
    "left_thigh": (2.0, 0.0, 0.0),
    "left_shin": (0.0, -6.0, 0.0),
    "right_thigh": (-2.0, 0.0, 0.0),
    "right_shin": (0.0, -6.0, 0.0),
}

_RUNTIME_NAME = re.compile(r"[a-z][a-z0-9_/-]{0,63}")
_RIG_ID = re.compile(r"[a-z0-9_.-]+:[a-z0-9_./-]+")
_KNOWN_CONTROL_NAMES = {
    "left_hand_ik",
    "right_hand_ik",
    "left_elbow_pole",
    "right_elbow_pole",
    "left_foot_ik",
    "right_foot_ik",
    "left_knee_pole",
    "right_knee_pole",
}


@dataclass(frozen=True)
class BoneSchemaEntry:
    runtime_name: str
    source_name: str
    parent_index: int
    aliases: tuple[str, ...]
    classification: str
    deform: bool


@dataclass(frozen=True)
class SkeletonSchema:
    rig_resource_id: str
    schema_id: str
    bones: tuple[BoneSchemaEntry, ...]
    legacy_compatible: bool = False

    @property
    def runtime_names(self) -> tuple[str, ...]:
        return tuple(bone.runtime_name for bone in self.bones)

    @property
    def source_names(self) -> tuple[str, ...]:
        return tuple(bone.source_name for bone in self.bones)

    @property
    def root_indices(self) -> tuple[int, ...]:
        return tuple(index for index, bone in enumerate(self.bones) if bone.parent_index == -1)

    @property
    def root_index(self) -> int:
        roots = self.root_indices
        if not roots:
            raise RuntimeError("Runtime skeleton must contain at least one root bone")
        for preferred in ("rig_root", "root"):
            for index in roots:
                if self.bones[index].runtime_name == preferred:
                    return index
        return roots[0]

    def index_of_runtime(self, name: str) -> int:
        for index, bone in enumerate(self.bones):
            if bone.runtime_name == name or name in bone.aliases:
                return index
        raise KeyError(name)


def _property(owner: Any, name: str, default: Any = None) -> Any:
    value = getattr(owner, name, None)
    if value not in (None, ""):
        return value
    getter = getattr(owner, "get", None)
    if getter is not None:
        return getter(name, default)
    return default


def _stored_property(owner: Any, name: str, default: Any = None) -> Any:
    """Read only an explicitly stored ID property, ignoring RNA defaults."""
    getter = getattr(owner, "get", None)
    return getter(name, default) if getter is not None else default


def rig_resource_id(armature: Any) -> str:
    data = getattr(armature, "data", armature)
    stored = _stored_property(data, "echo_rig_id", None)
    if stored in (None, ""):
        stored = _stored_property(armature, "echo_rig_id", None)
    rna_value = getattr(data, "echo_rig_id", None)
    if is_therig_2_6(armature) and stored in (None, "", LEGACY_RIG_ID) and rna_value in (
        None, "", LEGACY_RIG_ID
    ):
        resolved = THERIG_RIG_ID
    elif stored not in (None, ""):
        resolved = stored
    else:
        resolved = rna_value or LEGACY_RIG_ID
    value = str(resolved).strip()
    if not _RIG_ID.fullmatch(value):
        raise RuntimeError(
            f"Invalid Echo rig resource ID {value!r}; expected namespace:path"
        )
    return value


def schema_mode(armature: Any) -> str:
    data = getattr(armature, "data", armature)
    stored = _stored_property(data, "echo_rig_schema", None)
    if stored in (None, ""):
        stored = _stored_property(armature, "echo_rig_schema", None)
    rna_value = getattr(data, "echo_rig_schema", None)
    if is_therig_2_6(armature) and stored in (None, "", LEGACY_SCHEMA_ID, LEGACY_V1_SCHEMA_ID) and rna_value in (
        None, "", LEGACY_SCHEMA_ID, LEGACY_V1_SCHEMA_ID
    ):
        resolved = THERIG_SCHEMA_ID
    elif stored not in (None, ""):
        resolved = stored
    else:
        names = {bone.name for bone in _data_bones(armature)}
        if "body" in names and "body_lower" not in names:
            resolved = LEGACY_V1_SCHEMA_ID
        else:
            resolved = rna_value or LEGACY_SCHEMA_ID
    value = str(resolved).strip().upper()
    if value not in {LEGACY_SCHEMA_ID, LEGACY_V1_SCHEMA_ID, AUTO_SCHEMA_ID, THERIG_SCHEMA_ID}:
        raise RuntimeError(
            f"Unknown echo_rig_schema {value!r}; use {LEGACY_SCHEMA_ID}, {LEGACY_V1_SCHEMA_ID}, "
            f"{THERIG_SCHEMA_ID}, or {AUTO_SCHEMA_ID}"
        )
    return value


def _data_bones(armature: Any) -> list[Any]:
    data = getattr(armature, "data", armature)
    return list(getattr(data, "bones", ()))


def _bone_map(armature: Any) -> dict[str, Any]:
    return {bone.name: bone for bone in _data_bones(armature)}


def _aliases(bone: Any) -> tuple[str, ...]:
    raw = _property(bone, "echo_aliases", "")
    if isinstance(raw, str):
        values = re.split(r"[,;|\s]+", raw)
    elif isinstance(raw, Iterable):
        values = [str(value) for value in raw]
    else:
        values = []
    return tuple(dict.fromkeys(value.strip() for value in values if value.strip()))


def _runtime_name(bone: Any) -> str:
    return str(_property(bone, "echo_runtime_name", bone.name)).strip()


def _bone_class(bone: Any) -> str:
    explicit = str(_property(bone, "echo_bone_class", "")).strip().upper()
    if explicit in {"CONTROL", "RUNTIME", "DEFORM"}:
        return explicit
    if bone.name in _KNOWN_CONTROL_NAMES:
        return "CONTROL"
    return "DEFORM" if bool(getattr(bone, "use_deform", False)) else "RUNTIME"


def _fixed_legacy_schema(
    armature: Any,
    rig_id: str,
    schema_id: str,
    bone_order: tuple[str, ...],
    parent_by_runtime: dict[str, str | None],
) -> SkeletonSchema:
    by_name = _bone_map(armature)
    source_by_runtime: dict[str, Any] = {}
    for runtime_name in bone_order:
        source = by_name.get(runtime_name)
        if source is None:
            matches = [
                bone for bone in by_name.values()
                if _runtime_name(bone) == runtime_name or runtime_name in _aliases(bone)
            ]
            if len(matches) == 1:
                source = matches[0]
        if source is None:
            raise RuntimeError(f"Missing Echo runtime bone: {runtime_name}")
        source_by_runtime[runtime_name] = source

    index_by_name = {name: index for index, name in enumerate(bone_order)}
    entries = []
    for runtime_name in bone_order:
        source = source_by_runtime[runtime_name]
        parent_name = parent_by_runtime[runtime_name]
        entries.append(
            BoneSchemaEntry(
                runtime_name=runtime_name,
                source_name=source.name,
                parent_index=-1 if parent_name is None else index_by_name[parent_name],
                aliases=tuple(dict.fromkeys((source.name, *_aliases(source)))),
                classification=_bone_class(source),
                deform=bool(getattr(source, "use_deform", False)),
            )
        )
    return SkeletonSchema(rig_id, schema_id, tuple(entries), True)


def _legacy_v1_schema(armature: Any, rig_id: str) -> SkeletonSchema:
    return _fixed_legacy_schema(
        armature, rig_id, LEGACY_V1_SCHEMA_ID, LEGACY_V1_BONE_ORDER, LEGACY_V1_PARENT
    )


def _legacy_schema(armature: Any, rig_id: str) -> SkeletonSchema:
    return _fixed_legacy_schema(
        armature, rig_id, LEGACY_SCHEMA_ID, LEGACY_BONE_ORDER, LEGACY_PARENT
    )


def _runtime_candidates(armature: Any) -> dict[str, Any]:
    candidates: dict[str, Any] = {}
    for bone in _data_bones(armature):
        explicit = bool(_property(bone, "echo_runtime", False))
        if (bool(getattr(bone, "use_deform", False)) or explicit) and _bone_class(bone) != "CONTROL":
            candidates[bone.name] = bone

    if not candidates:
        raise RuntimeError("Armature has no deform or explicitly marked Echo runtime bones")

    for bone in tuple(candidates.values()):
        parent = getattr(bone, "parent", None)
        while parent is not None:
            if _bone_class(parent) != "CONTROL":
                candidates[parent.name] = parent
            parent = getattr(parent, "parent", None)
    return candidates


def _nearest_parent_name(bone: Any, included: dict[str, Any]) -> str | None:
    parent = getattr(bone, "parent", None)
    while parent is not None:
        if parent.name in included:
            return parent.name
        parent = getattr(parent, "parent", None)
    return None


def _explicit_order(candidates: dict[str, Any]) -> list[str] | None:
    marked = {
        name: int(_property(bone, "echo_runtime_index", -1))
        for name, bone in candidates.items()
        if int(_property(bone, "echo_runtime_index", -1)) >= 0
    }
    if not marked:
        return None
    if len(marked) != len(candidates):
        raise RuntimeError(
            "If one bone has echo_runtime_index, every runtime bone must have one"
        )
    values = sorted(marked.values())
    if values != list(range(len(candidates))):
        raise RuntimeError("echo_runtime_index values must be unique and contiguous from 0")
    return [name for name, _index in sorted(marked.items(), key=lambda item: item[1])]


def _parent_first_order(
    candidates: dict[str, Any],
    runtime_name_resolver=_runtime_name,
) -> list[str]:
    parent_by_name = {
        name: _nearest_parent_name(bone, candidates)
        for name, bone in candidates.items()
    }
    children: dict[str | None, list[str]] = {}
    for name, parent in parent_by_name.items():
        children.setdefault(parent, []).append(name)

    def sort_key(name: str):
        runtime = runtime_name_resolver(candidates[name])
        root_priority = 0 if runtime == "rig_root" else 1 if runtime == "root" else 2
        return root_priority, runtime, name

    for values in children.values():
        values.sort(key=sort_key)

    roots = children.get(None, [])
    if not roots:
        raise RuntimeError("Runtime skeleton must contain at least one root")
    result: list[str] = []

    def visit(name: str) -> None:
        result.append(name)
        for child in children.get(name, ()):
            visit(child)

    for root in roots:
        visit(root)
    return result


def _auto_schema(armature: Any, rig_id: str) -> SkeletonSchema:
    candidates = _runtime_candidates(armature)
    order = _explicit_order(candidates) or _parent_first_order(candidates)
    index_by_source = {name: index for index, name in enumerate(order)}
    entries = []
    runtime_names: set[str] = set()
    for source_name in order:
        bone = candidates[source_name]
        runtime_name = _runtime_name(bone)
        if not _RUNTIME_NAME.fullmatch(runtime_name):
            raise RuntimeError(
                f"Runtime bone name {runtime_name!r} must be lower snake_case"
            )
        if runtime_name in runtime_names:
            raise RuntimeError(f"Duplicate runtime bone name: {runtime_name}")
        runtime_names.add(runtime_name)
        parent_name = _nearest_parent_name(bone, candidates)
        entries.append(
            BoneSchemaEntry(
                runtime_name=runtime_name,
                source_name=source_name,
                parent_index=-1 if parent_name is None else index_by_source[parent_name],
                aliases=tuple(dict.fromkeys((source_name, *_aliases(bone)))),
                classification=_bone_class(bone),
                deform=bool(getattr(bone, "use_deform", False)),
            )
        )
    return SkeletonSchema(rig_id, AUTO_SCHEMA_ID, tuple(entries), False)


def _therig_schema(armature: Any, rig_id: str) -> SkeletonSchema:
    if not is_therig_2_6(armature):
        raise RuntimeError("TheRig 2.6 profile does not match this armature")

    by_name = _bone_map(armature)
    candidates = _runtime_candidates(armature)

    # Item/socket anchors do not need to deform vertices, but they must survive
    # into the runtime skeleton so Minecraft attachments can follow the exact
    # evaluated hand transform. Include their full ancestry as well.
    for source_name in THERIG_EXPLICIT_RUNTIME_BONES:
        source = by_name.get(source_name)
        if source is None:
            continue
        current = source
        while current is not None:
            candidates[current.name] = current
            current = getattr(current, "parent", None)

    order = _parent_first_order(
        candidates,
        runtime_name_resolver=lambda bone: therig_runtime_name(bone.name),
    )
    index_by_source = {name: index for index, name in enumerate(order)}
    entries = []
    runtime_names: dict[str, str] = {}
    for source_name in order:
        source = candidates[source_name]
        runtime_name = therig_runtime_name(source_name)
        if not _RUNTIME_NAME.fullmatch(runtime_name):
            raise RuntimeError(
                f"TheRig runtime bone name {runtime_name!r} derived from {source_name!r} is invalid"
            )
        previous = runtime_names.get(runtime_name)
        if previous is not None and previous != source_name:
            raise RuntimeError(
                f"TheRig bones {previous!r} and {source_name!r} collapse to duplicate "
                f"runtime name {runtime_name!r}"
            )
        runtime_names[runtime_name] = source_name
        parent_name = _nearest_parent_name(source, candidates)
        entries.append(
            BoneSchemaEntry(
                runtime_name=runtime_name,
                source_name=source_name,
                parent_index=-1 if parent_name is None else index_by_source[parent_name],
                aliases=tuple(dict.fromkeys((source_name, *_aliases(source)))),
                classification=_bone_class(source),
                deform=bool(getattr(source, "use_deform", False)),
            )
        )
    return SkeletonSchema(rig_id, THERIG_SCHEMA_ID, tuple(entries), False)


def validate_schema(schema: SkeletonSchema) -> None:
    if not schema.bones or len(schema.bones) > MAX_RUNTIME_BONES:
        raise RuntimeError(
            f"Runtime skeleton must contain between 1 and {MAX_RUNTIME_BONES} bones"
        )
    schema.root_index
    for index, bone in enumerate(schema.bones):
        if bone.parent_index < -1 or bone.parent_index >= len(schema.bones):
            raise RuntimeError(f"Invalid parent index for bone {bone.runtime_name}")
        seen = {index}
        parent = bone.parent_index
        while parent >= 0:
            if parent in seen:
                raise RuntimeError(f"Cyclic runtime skeleton at bone {bone.runtime_name}")
            seen.add(parent)
            parent = schema.bones[parent].parent_index


def resolve_skeleton_schema(armature: Any) -> SkeletonSchema:
    rig_id = rig_resource_id(armature)
    mode = schema_mode(armature)
    if mode == LEGACY_SCHEMA_ID:
        schema = _legacy_schema(armature, rig_id)
    elif mode == LEGACY_V1_SCHEMA_ID:
        schema = _legacy_v1_schema(armature, rig_id)
    elif mode == THERIG_SCHEMA_ID:
        schema = _therig_schema(armature, rig_id)
    else:
        schema = _auto_schema(armature, rig_id)
    validate_schema(schema)
    return schema
