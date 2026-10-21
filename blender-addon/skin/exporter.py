from __future__ import annotations

from dataclasses import dataclass

from ..rig.schema import SkeletonSchema, resolve_skeleton_schema
from .mesh_reader import read_mesh
from .skeleton_reader import read_skeleton
from .validation import validate_mesh_sources, validate_model
from .lattice_weights import armature_for_lattice


@dataclass(frozen=True)
class SkinExportResult:
    model: dict
    schema: SkeletonSchema
    armature: object
    mesh_objects: tuple
    warnings: tuple[str, ...]

    @property
    def vertex_count(self) -> int:
        return sum(len(mesh["vertices"]) for mesh in self.model["meshes"])

    @property
    def triangle_count(self) -> int:
        return sum(len(mesh["triangles"]) // 3 for mesh in self.model["meshes"])


def armature_for_mesh(mesh_object):
    targets = {
        modifier.object
        for modifier in mesh_object.modifiers
        if modifier.type == "ARMATURE"
        and modifier.object is not None
        and modifier.object.type == "ARMATURE"
    }
    for modifier in mesh_object.modifiers:
        lattice = modifier.object if modifier.type == "LATTICE" else None
        if lattice is None or lattice.type != "LATTICE":
            continue
        target = armature_for_lattice(lattice)
        if target is not None:
            targets.add(target)
    if len(targets) != 1:
        raise RuntimeError(
            f"Mesh {mesh_object.name} must resolve to exactly one Armature "
            "through an Armature or armature-driven Lattice modifier"
        )
    return next(iter(targets))


def mesh_objects_from_context(context) -> tuple:
    selected = tuple(obj for obj in context.selected_objects if obj.type == "MESH")
    if selected:
        return selected
    active = context.object
    if active is not None and active.type == "MESH":
        return (active,)
    if active is not None and active.type == "ARMATURE":
        attached = []
        for obj in context.scene.objects:
            if (
                obj.type != "MESH"
                or obj.hide_render
                or not obj.visible_get(view_layer=context.view_layer)
                or not obj.data.polygons
            ):
                continue
            try:
                attached_armature = armature_for_mesh(obj)
            except RuntimeError:
                continue
            if attached_armature == active:
                attached.append(obj)
        return tuple(sorted(attached, key=lambda obj: obj.name))
    return ()


def build_skin_model(mesh_objects) -> SkinExportResult:
    mesh_objects = tuple(mesh_objects)
    if not mesh_objects:
        raise RuntimeError(
            "Select one or more meshes with an Armature or armature-driven Lattice modifier"
        )
    armatures = {armature_for_mesh(obj) for obj in mesh_objects}
    if len(armatures) != 1:
        raise RuntimeError("All exported meshes must use the same armature")
    armature = next(iter(armatures))
    validate_mesh_sources(mesh_objects, armature)
    schema = resolve_skeleton_schema(armature)
    warnings: list[str] = []
    model = {
        "formatVersion": 2,
        "bones": read_skeleton(armature, schema),
        "meshes": [
            read_mesh(obj, armature, schema, warnings)
            for obj in sorted(mesh_objects, key=lambda item: item.name)
        ],
    }
    validate_model(model)
    return SkinExportResult(
        model=model,
        schema=schema,
        armature=armature,
        mesh_objects=mesh_objects,
        warnings=tuple(warnings),
    )
