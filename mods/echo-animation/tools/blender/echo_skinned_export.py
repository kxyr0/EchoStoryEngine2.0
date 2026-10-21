"""Blender CLI exporter for Echo Animation *.skin.json v2.

Usage: blender --background model.blend --python echo_skinned_export.py -- output.skin.json [MeshName]
"""

import json
import pathlib
import re
import sys

import bpy
from mathutils import Matrix


def fail(message):
    raise RuntimeError("Echo skin export: " + message)


def warn(message):
    print("Echo skin export WARNING: " + message)


def uniform_scale(obj):
    scale = [abs(value) for value in obj.matrix_world.to_scale()]
    if max(scale) - min(scale) > 1.0e-5:
        fail(f"{obj.name} has unsupported non-uniform scale {scale}")


def coordinate_matrix():
    # Blender (X right, Y forward, Z up) -> Minecraft pixels (X right, Y down, Z forward).
    return Matrix(((16, 0, 0, 0), (0, 0, -16, 0), (0, 16, 0, 0), (0, 0, 0, 1)))


def matrix_values(matrix):
    return [matrix[row][column] for column in range(4) for row in range(4)]


def deform_parent(bone, indices):
    parent = bone.parent
    while parent is not None and parent.name not in indices:
        parent = parent.parent
    return -1 if parent is None else indices[parent.name]


def validate_hierarchy(bones, indices):
    roots = [bone for bone in bones if deform_parent(bone, indices) == -1]
    if len(roots) != 1:
        fail(f"expected one deform root, found {[bone.name for bone in roots]}")
    for bone in bones:
        seen = set()
        current = bone
        while current is not None:
            if current.name in seen:
                fail(f"cyclic hierarchy at {bone.name}")
            seen.add(current.name)
            current = current.parent


def skeleton(armature):
    bones = [bone for bone in armature.data.bones if bone.use_deform]
    if not bones:
        fail(f"armature {armature.name} has no deform bones")
    names = [bone.name for bone in bones]
    if len(names) != len(set(names)):
        fail("duplicate deform bone names")
    invalid = [name for name in names if not re.fullmatch(r"[a-z][a-z0-9_/-]{0,63}", name)]
    if invalid:
        fail(f"bone names must use lower snake_case: {invalid}")
    indices = {name: index for index, name in enumerate(names)}
    validate_hierarchy(bones, indices)
    conversion = coordinate_matrix()
    inverse_conversion = conversion.inverted()
    result = []
    for bone in bones:
        parent_index = deform_parent(bone, indices)
        local = bone.matrix_local if parent_index < 0 else bones[parent_index].matrix_local.inverted() @ bone.matrix_local
        bind_local = conversion @ local @ inverse_conversion
        bind_global = conversion @ bone.matrix_local @ inverse_conversion
        if abs(bind_global.determinant()) < 1.0e-8:
            fail(f"invalid bind pose for bone {bone.name}")
        result.append({"name": bone.name, "parent": parent_index,
                       "bindLocal": matrix_values(bind_local),
                       "inverseBind": matrix_values(bind_global.inverted())})
    return result, indices


def influences(obj, vertex, bone_indices, root_index, reported_groups):
    weighted = []
    for assignment in vertex.groups:
        if assignment.weight <= 0:
            continue
        name = obj.vertex_groups[assignment.group].name
        if name not in bone_indices:
            if name not in reported_groups:
                warn(f"vertex group {name!r} has no deform bone and is ignored")
                reported_groups.add(name)
            continue
        weighted.append((bone_indices[name], assignment.weight))
    weighted.sort(key=lambda item: item[1], reverse=True)
    if len(weighted) > 4:
        warn(f"mesh {obj.name} vertex {vertex.index} has {len(weighted)} influences; keeping four")
        weighted = weighted[:4]
    total = sum(weight for _, weight in weighted)
    if total <= 1.0e-8:
        warn(f"mesh {obj.name} vertex {vertex.index} has no weights; attaching to root")
        weighted = [(root_index, 1.0)]
        total = 1.0
    return [bone for bone, _ in weighted], [weight / total for _, weight in weighted]


def mesh_data(obj, armature, bone_indices, root_index):
    mesh = obj.data
    mesh.calc_loop_triangles()
    if not mesh.uv_layers.active:
        fail(f"mesh {obj.name} has no active UV layer")
    conversion = coordinate_matrix()
    relative = armature.matrix_world.inverted() @ obj.matrix_world
    point_matrix = conversion @ relative
    normal_matrix = point_matrix.to_3x3().inverted().transposed()
    uv_data = mesh.uv_layers.active.data
    vertices = []
    triangles = []
    reported_groups = set()
    for triangle in mesh.loop_triangles:
        for loop_index in triangle.loops:
            loop = mesh.loops[loop_index]
            source = mesh.vertices[loop.vertex_index]
            position = point_matrix @ source.co
            normal = (normal_matrix @ loop.normal).normalized()
            uv = uv_data[loop_index].uv
            indices, weights = influences(obj, source, bone_indices, root_index, reported_groups)
            vertices.append({"position": list(position), "normal": list(normal),
                             "uv": [uv.x, 1.0 - uv.y],
                             "boneIndices": indices, "boneWeights": weights})
            triangles.append(len(vertices) - 1)
    return {"name": obj.name, "vertices": vertices, "triangles": triangles}


def armature_for(obj):
    modifiers = [modifier for modifier in obj.modifiers if modifier.type == "ARMATURE" and modifier.object]
    if len(modifiers) != 1:
        fail(f"mesh {obj.name} must have exactly one Armature modifier")
    return modifiers[0].object


def export(output, mesh_name=None):
    obj = bpy.data.objects.get(mesh_name) if mesh_name else bpy.context.active_object
    if obj is None or obj.type != "MESH":
        fail("select a mesh or pass its object name")
    obj.update_from_editmode()
    armature = armature_for(obj)
    uniform_scale(obj)
    uniform_scale(armature)
    bones, indices = skeleton(armature)
    root_index = next(index for index, bone in enumerate(bones) if bone["parent"] == -1)
    model = {"formatVersion": 2, "bones": bones,
             "meshes": [mesh_data(obj, armature, indices, root_index)]}
    pathlib.Path(output).write_text(json.dumps(model, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Echo skin export: wrote {output}; bones={len(bones)} vertices={len(model['meshes'][0]['vertices'])}")


if __name__ == "__main__":
    arguments = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    if not arguments:
        fail("usage: -- output.skin.json [MeshName]")
    export(arguments[0], arguments[1] if len(arguments) > 1 else None)
