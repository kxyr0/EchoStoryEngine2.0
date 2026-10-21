from __future__ import annotations

import math

from mathutils import Matrix

from .coordinate_converter import convert_local_matrix


def column_major_values(matrix: Matrix) -> list[float]:
    values = [float(matrix[row][column]) for column in range(4) for row in range(4)]
    if not all(math.isfinite(value) for value in values):
        raise RuntimeError("Matrix contains a non-finite value")
    return values


def source_global_bind(armature, source_name: str) -> Matrix:
    return armature.data.bones[source_name].matrix_local.copy()


def source_local_bind(armature, schema, bone_index: int) -> Matrix:
    entry = schema.bones[bone_index]
    child = source_global_bind(armature, entry.source_name)
    if entry.parent_index < 0:
        return child
    parent_name = schema.bones[entry.parent_index].source_name
    parent = source_global_bind(armature, parent_name)
    return parent.inverted_safe() @ child


def echo_bind_matrices(armature, schema, bone_index: int) -> tuple[Matrix, Matrix]:
    local = convert_local_matrix(source_local_bind(armature, schema, bone_index))
    global_bind = convert_local_matrix(
        source_global_bind(armature, schema.bones[bone_index].source_name)
    )
    if abs(global_bind.determinant()) < 1.0e-8:
        raise RuntimeError(
            f"Invalid bind pose for bone {schema.bones[bone_index].runtime_name}"
        )
    return local, global_bind.inverted_safe()
