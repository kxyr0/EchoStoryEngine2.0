from __future__ import annotations

from mathutils import Matrix


PIXELS_PER_BLENDER_UNIT = 16.0


def blender_to_echo_matrix() -> Matrix:
    """Blender X/right, Y/forward, Z/up -> Echo X/right, Y/down, Z/forward."""
    return Matrix((
        (PIXELS_PER_BLENDER_UNIT, 0.0, 0.0, 0.0),
        (0.0, 0.0, -PIXELS_PER_BLENDER_UNIT, 0.0),
        (0.0, PIXELS_PER_BLENDER_UNIT, 0.0, 0.0),
        (0.0, 0.0, 0.0, 1.0),
    ))


def convert_local_matrix(matrix: Matrix) -> Matrix:
    conversion = blender_to_echo_matrix()
    return conversion @ matrix @ conversion.inverted()


def mesh_point_matrix(mesh_object, armature_object) -> Matrix:
    relative = armature_object.matrix_world.inverted_safe() @ mesh_object.matrix_world
    return blender_to_echo_matrix() @ relative


def mesh_normal_matrix(mesh_object, armature_object) -> Matrix:
    return mesh_point_matrix(mesh_object, armature_object).to_3x3().inverted_safe().transposed()
