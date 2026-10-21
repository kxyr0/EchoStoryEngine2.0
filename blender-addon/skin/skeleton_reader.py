from __future__ import annotations

from ..rig.matrix_converter import column_major_values, echo_bind_matrices


def read_skeleton(armature, schema) -> list[dict]:
    bones = []
    for index, entry in enumerate(schema.bones):
        bind_local, inverse_bind = echo_bind_matrices(armature, schema, index)
        bones.append(
            {
                "name": entry.runtime_name,
                "parent": entry.parent_index,
                "bindLocal": column_major_values(bind_local),
                "inverseBind": column_major_values(inverse_bind),
            }
        )
    return bones
