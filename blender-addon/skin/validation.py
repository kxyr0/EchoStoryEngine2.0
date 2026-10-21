from __future__ import annotations

import math

from mathutils import Matrix


ROOT_FIELDS = {"formatVersion", "bones", "meshes"}
BONE_FIELDS = {"name", "parent", "bindLocal", "inverseBind"}
MESH_FIELDS = {"name", "vertices", "triangles"}
VERTEX_FIELDS = {"position", "normal", "uv", "boneIndices", "boneWeights"}
EPSILON = 0.001


def validate_uniform_object_scale(obj) -> None:
    scale = [abs(float(value)) for value in obj.matrix_world.to_scale()]
    if not all(math.isfinite(value) and value > 1.0e-8 for value in scale):
        raise RuntimeError(f"{obj.name} has invalid object scale {scale}")
    if max(scale) - min(scale) > 1.0e-5:
        raise RuntimeError(f"{obj.name} has unsupported non-uniform object scale {scale}")


def validate_invertible_object_scale(obj) -> None:
    scale = [abs(float(value)) for value in obj.matrix_world.to_scale()]
    if not all(math.isfinite(value) and value > 1.0e-8 for value in scale):
        raise RuntimeError(f"{obj.name} has invalid object scale {scale}")


def validate_mesh_sources(mesh_objects, armature) -> None:
    if not mesh_objects:
        raise RuntimeError("Select at least one skinned mesh")
    names = [obj.name for obj in mesh_objects]
    if len(names) != len(set(names)):
        raise RuntimeError("Skinned mesh names must be unique")
    validate_uniform_object_scale(armature)
    for obj in mesh_objects:
        # Mesh/object transforms are baked into exported positions and normals,
        # so non-uniform mesh scale is safe. The armature itself must remain
        # uniform because its matrices define the shared runtime palette.
        validate_invertible_object_scale(obj)


def _matrix(values: list[float]) -> Matrix:
    if len(values) != 16:
        raise RuntimeError(f"Expected 16 matrix values, got {len(values)}")
    return Matrix(
        tuple(
            tuple(float(values[column * 4 + row]) for column in range(4))
            for row in range(4)
        )
    )


def _identity(matrix: Matrix) -> bool:
    for row in range(4):
        for column in range(4):
            expected = 1.0 if row == column else 0.0
            value = float(matrix[row][column])
            if not math.isfinite(value) or abs(value - expected) > EPSILON:
                return False
    return True


def _validate_bind_pose(model: dict) -> None:
    bones = model["bones"]
    globals_: list[Matrix | None] = [None] * len(bones)

    def resolve(index: int, path: set[int]) -> Matrix:
        existing = globals_[index]
        if existing is not None:
            return existing
        if index in path:
            raise RuntimeError(f"Cyclic skeleton at bone {bones[index]['name']}")
        path.add(index)
        bone = bones[index]
        local = _matrix(bone["bindLocal"])
        parent = int(bone["parent"])
        value = local if parent < 0 else resolve(parent, path) @ local
        globals_[index] = value
        path.remove(index)
        return value

    for index, bone in enumerate(bones):
        restored = resolve(index, set()) @ _matrix(bone["inverseBind"])
        if not _identity(restored):
            raise RuntimeError(f"Inverse bind mismatch for bone {bone['name']}")


def validate_model(model: dict, bone_limit: int = 512) -> None:
    if set(model) != ROOT_FIELDS:
        raise RuntimeError(f"Unexpected root JSON fields: {sorted(set(model) ^ ROOT_FIELDS)}")
    if model["formatVersion"] != 2:
        raise RuntimeError("Echo skinned model formatVersion must be 2")

    bones = model["bones"]
    if not 1 <= len(bones) <= bone_limit:
        raise RuntimeError(f"Bone count must be between 1 and {bone_limit}")
    names: set[str] = set()
    roots = 0
    for bone in bones:
        if set(bone) != BONE_FIELDS:
            raise RuntimeError(f"Unexpected bone JSON fields in {bone.get('name', '?')}")
        if bone["name"] in names:
            raise RuntimeError(f"Duplicate bone {bone['name']}")
        names.add(bone["name"])
        parent = int(bone["parent"])
        roots += parent == -1
        if parent < -1 or parent >= len(bones):
            raise RuntimeError(f"Invalid parent for bone {bone['name']}")
        _matrix(bone["bindLocal"])
        _matrix(bone["inverseBind"])
    if roots < 1:
        raise RuntimeError("Skeleton must contain at least one root")

    meshes = model["meshes"]
    if not meshes:
        raise RuntimeError("Model must contain at least one mesh")
    for mesh in meshes:
        if set(mesh) != MESH_FIELDS:
            raise RuntimeError(f"Unexpected mesh JSON fields in {mesh.get('name', '?')}")
        vertices = mesh["vertices"]
        triangles = mesh["triangles"]
        if not vertices:
            raise RuntimeError(f"Mesh {mesh['name']} has no vertices")
        if len(triangles) % 3:
            raise RuntimeError(f"Mesh {mesh['name']} index count is not divisible by three")
        if any(index < 0 or index >= len(vertices) for index in triangles):
            raise RuntimeError(f"Mesh {mesh['name']} has an invalid vertex index")
        for vertex_index, vertex in enumerate(vertices):
            if set(vertex) != VERTEX_FIELDS:
                raise RuntimeError(
                    f"Unexpected vertex JSON fields in {mesh['name']} vertex {vertex_index}"
                )
            if len(vertex["position"]) != 3 or len(vertex["normal"]) != 3 or len(vertex["uv"]) != 2:
                raise RuntimeError(f"Invalid vertex vector size in {mesh['name']} vertex {vertex_index}")
            indices = vertex["boneIndices"]
            weights = vertex["boneWeights"]
            if not 1 <= len(indices) == len(weights) <= 4:
                raise RuntimeError(f"Invalid skin slots in {mesh['name']} vertex {vertex_index}")
            if any(index < 0 or index >= len(bones) for index in indices):
                raise RuntimeError(f"Invalid bone index in {mesh['name']} vertex {vertex_index}")
            if any(not math.isfinite(weight) or weight < 0.0 for weight in weights):
                raise RuntimeError(f"Invalid bone weight in {mesh['name']} vertex {vertex_index}")
            if abs(sum(weights) - 1.0) > EPSILON:
                raise RuntimeError(f"Weights do not sum to one in {mesh['name']} vertex {vertex_index}")
    _validate_bind_pose(model)
