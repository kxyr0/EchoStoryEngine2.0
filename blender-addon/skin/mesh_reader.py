from __future__ import annotations

import math

from ..rig.coordinate_converter import mesh_normal_matrix, mesh_point_matrix
from .triangulation import loop_triangles
from .lattice_weights import lattice_group_weights, lattice_modifier_for_mesh
from .weight_reader import (
    group_bone_weights,
    read_vertex_weights,
    source_group_weights,
    vertex_group_bone_indices,
)
from ..rig.therig import THERIG_SCHEMA_ID, therig_lattice_fallback


def _finite(values, label: str) -> list[float]:
    result = [float(value) for value in values]
    if not all(math.isfinite(value) for value in result):
        raise RuntimeError(f"{label} contains a non-finite value")
    return result


def read_mesh(mesh_object, armature, schema, warnings: list[str]) -> dict:
    mesh_object.update_from_editmode()
    mesh = mesh_object.data
    triangles = loop_triangles(mesh)
    uv_layer = mesh.uv_layers.active
    if uv_layer is None:
        raise RuntimeError(f"Mesh {mesh_object.name} has no active UV layer")

    point_matrix = mesh_point_matrix(mesh_object, armature)
    normal_matrix = mesh_normal_matrix(mesh_object, armature)
    vertices: list[dict] = []
    indices: list[int] = []
    reported_groups: set[str] = set()
    group_to_bone = vertex_group_bone_indices(schema)
    runtime_index = {
        entry.runtime_name: index for index, entry in enumerate(schema.bones)
    }
    lattice_pair = lattice_modifier_for_mesh(mesh_object, armature)
    lattice = lattice_pair[1] if lattice_pair is not None else None
    lattice_fallback = schema.root_index
    if lattice is not None and schema.schema_id == THERIG_SCHEMA_ID:
        lattice_fallback = runtime_index[therig_lattice_fallback(lattice.name)]
    influence_cache: dict[int, tuple[list[int], list[float]]] = {}

    for triangle in triangles:
        for loop_index in triangle.loops:
            loop = mesh.loops[loop_index]
            source = mesh.vertices[loop.vertex_index]
            position = point_matrix @ source.co
            normal = normal_matrix @ loop.normal
            if normal.length_squared <= 1.0e-12:
                raise RuntimeError(
                    f"Mesh {mesh_object.name} loop {loop_index} has a zero normal"
                )
            normal.normalize()
            uv = uv_layer.data[loop_index].uv
            weights = influence_cache.get(source.index)
            if weights is None:
                direct_groups = source_group_weights(mesh_object, source)
                direct_is_usable = any(
                    group_bone_weights(schema, name, group_to_bone)
                    for name, weight in direct_groups.items()
                    if weight > 0.0
                )
                transferred_groups = (
                    lattice_group_weights(mesh_object, source, lattice)
                    if lattice is not None and not direct_is_usable
                    else direct_groups
                )
                weights = read_vertex_weights(
                    mesh_object,
                    source,
                    schema,
                    group_to_bone,
                    schema.root_index,
                    warnings,
                    reported_groups,
                    group_weights=transferred_groups,
                    fallback_bone_index=lattice_fallback if lattice is not None else None,
                )
                influence_cache[source.index] = weights
            bone_indices, bone_weights = weights
            vertices.append(
                {
                    "position": _finite(position, f"Mesh {mesh_object.name} position"),
                    "normal": _finite(normal, f"Mesh {mesh_object.name} normal"),
                    "uv": _finite((uv.x, 1.0 - uv.y), f"Mesh {mesh_object.name} UV"),
                    "boneIndices": bone_indices,
                    "boneWeights": bone_weights,
                }
            )
            indices.append(len(vertices) - 1)

    return {
        "name": mesh_object.name,
        "vertices": vertices,
        "triangles": indices,
    }
