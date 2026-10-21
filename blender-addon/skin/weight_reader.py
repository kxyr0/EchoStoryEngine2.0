from __future__ import annotations

import math

from ..rig.therig import THERIG_SCHEMA_ID


MAX_INFLUENCES = 4


def vertex_group_bone_indices(schema) -> dict[str, int]:
    result: dict[str, int] = {}
    for index, entry in enumerate(schema.bones):
        for name in (entry.runtime_name, entry.source_name, *entry.aliases):
            existing = result.get(name)
            if existing is not None and existing != index:
                raise RuntimeError(
                    f"Vertex-group alias {name!r} resolves to more than one runtime bone"
                )
            result[name] = index
    return result


def group_bone_weights(
    schema,
    group_name: str,
    group_to_bone: dict[str, int],
) -> dict[int, float]:
    exact = group_to_bone.get(group_name)
    if exact is not None:
        return {exact: 1.0}
    if schema.schema_id == THERIG_SCHEMA_ID:
        # TheRig v3 exports the real deform/lattice bones one-to-one. A missing
        # source group is therefore genuinely unsupported and must not be
        # silently collapsed onto a coarse limb/head proxy.
        return {}
    return {}


def source_group_weights(mesh_object, vertex) -> dict[str, float]:
    return {
        mesh_object.vertex_groups[assignment.group].name: float(assignment.weight)
        for assignment in vertex.groups
        if assignment.weight > 0.0
    }


def read_vertex_weights(
    mesh_object,
    vertex,
    schema,
    group_to_bone: dict[str, int],
    root_index: int,
    warnings: list[str],
    reported_groups: set[str],
    group_weights: dict[str, float] | None = None,
    fallback_bone_index: int | None = None,
) -> tuple[list[int], list[float]]:
    weighted_by_bone: dict[int, float] = {}
    raw_weights = group_weights if group_weights is not None else source_group_weights(mesh_object, vertex)
    for group_name, raw_weight in raw_weights.items():
        weight = float(raw_weight)
        if not math.isfinite(weight) or weight < 0.0:
            raise RuntimeError(
                f"Mesh {mesh_object.name} vertex {vertex.index} has invalid weight {weight}"
            )
        if weight == 0.0:
            continue
        targets = group_bone_weights(schema, group_name, group_to_bone)
        if not targets:
            if group_name not in reported_groups:
                warnings.append(
                    f"Mesh {mesh_object.name}: vertex group {group_name!r} has no runtime bone and was ignored"
                )
                reported_groups.add(group_name)
            continue
        for bone_index, factor in targets.items():
            weighted_by_bone[bone_index] = (
                weighted_by_bone.get(bone_index, 0.0) + weight * factor
            )

    weighted = list(weighted_by_bone.items())
    weighted.sort(key=lambda item: (-item[1], item[0]))
    if len(weighted) > MAX_INFLUENCES:
        warnings.append(
            f"Mesh {mesh_object.name} vertex {vertex.index}: kept four strongest of {len(weighted)} influences"
        )
        weighted = weighted[:MAX_INFLUENCES]

    total = sum(weight for _index, weight in weighted)
    if total <= 1.0e-8:
        attachment = root_index if fallback_bone_index is None else fallback_bone_index
        warnings.append(
            f"Mesh {mesh_object.name} vertex {vertex.index}: no runtime weights; "
            f"attached to bone {attachment}"
        )
        return [attachment], [1.0]

    return (
        [bone_index for bone_index, _weight in weighted],
        [weight / total for _bone_index, weight in weighted],
    )
