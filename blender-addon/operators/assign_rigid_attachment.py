from __future__ import annotations

import bpy
from bpy.props import EnumProperty

from ..rig.schema import resolve_skeleton_schema
from ..skin.exporter import armature_for_mesh


def _active_mesh(context):
    obj = context.object
    if obj is not None and obj.type == "MESH":
        return obj
    selected = [obj for obj in context.selected_objects if obj.type == "MESH"]
    return selected[0] if len(selected) == 1 else None


def _bone_items(_self, context):
    mesh = _active_mesh(context)
    if mesh is None:
        return [("__NONE__", "Нет меша со скинингом", "Выберите меш персонажа")]
    try:
        schema = resolve_skeleton_schema(armature_for_mesh(mesh))
        return [
            (
                entry.runtime_name,
                entry.runtime_name,
                f"Кость #{index}; исходная кость Blender: {entry.source_name}",
            )
            for index, entry in enumerate(schema.bones)
        ]
    except RuntimeError as error:
        return [("__NONE__", "Некорректный скелет", str(error))]


class ECHO_OT_assign_rigid_attachment(bpy.types.Operator):
    bl_idname = "echo.assign_rigid_attachment"
    bl_label = "Жёсткая привязка к кости"
    bl_description = "Привязать выбранные вершины на 100% к одной runtime-кости Echo. Полезно для жёстких деталей: очков, предметов, брони."
    bl_options = {"REGISTER", "UNDO"}

    bone_name: EnumProperty(name="Runtime-кость", description="Кость, которая будет полностью управлять выбранными вершинами", items=_bone_items)

    @classmethod
    def poll(cls, context):
        return _active_mesh(context) is not None

    def invoke(self, context, _event):
        return context.window_manager.invoke_props_dialog(self)

    def execute(self, context):
        mesh = _active_mesh(context)
        if mesh is None:
            self.report({"ERROR"}, "Выберите один меш")
            return {"CANCELLED"}
        if self.bone_name == "__NONE__":
            self.report({"ERROR"}, "Выберите корректную runtime-кость")
            return {"CANCELLED"}

        original_mode = mesh.mode
        try:
            schema = resolve_skeleton_schema(armature_for_mesh(mesh))
            entry = next(
                (bone for bone in schema.bones if bone.runtime_name == self.bone_name),
                None,
            )
            if entry is None:
                raise RuntimeError(f"Runtime-кость {self.bone_name!r} отсутствует в выбранной схеме")

            if original_mode == "EDIT":
                mesh.update_from_editmode()
                bpy.ops.object.mode_set(mode="OBJECT")
            vertex_indices = [vertex.index for vertex in mesh.data.vertices if vertex.select]
            if not vertex_indices:
                raise RuntimeError("Выберите хотя бы одну вершину меша")

            runtime_group_names = {
                name
                for bone in schema.bones
                for name in (bone.runtime_name, bone.source_name, *bone.aliases)
            }
            for group in mesh.vertex_groups:
                if group.name in runtime_group_names:
                    group.remove(vertex_indices)
            target = mesh.vertex_groups.get(entry.runtime_name)
            if target is None:
                target = mesh.vertex_groups.new(name=entry.runtime_name)
            target.add(vertex_indices, 1.0, "REPLACE")
            self.report(
                {"INFO"},
                f"Жёстко привязано вершин: {len(vertex_indices)} → {entry.runtime_name}",
            )
            return {"FINISHED"}
        except Exception as error:
            self.report({"ERROR"}, str(error))
            return {"CANCELLED"}
        finally:
            if original_mode == "EDIT" and mesh.mode != "EDIT":
                bpy.ops.object.mode_set(mode="EDIT")


RIGID_ATTACHMENT_CLASSES = (ECHO_OT_assign_rigid_attachment,)
