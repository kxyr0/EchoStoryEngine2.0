from __future__ import annotations

from pathlib import Path

import bpy
from bpy.props import BoolProperty, StringProperty
from bpy_extras.io_utils import ExportHelper

from ..rig.schema import resolve_skeleton_schema
from ..rig.writer import write_rig_definition
from ..rig.therig import THERIG_SCHEMA_ID
from ..skin.exporter import armature_for_mesh, build_skin_model, mesh_objects_from_context
from ..skin.writer import write_skin_json


class ECHO_OT_export_skin(bpy.types.Operator, ExportHelper):
    bl_idname = "echo.export_skin"
    bl_label = "Экспортировать модель Echo"
    bl_description = "Экспортировать меш со скинингом и полный deform/lattice-скелет в Echo *.skin.json v2. Для обычной анимации эта кнопка не нужна."
    bl_options = {"REGISTER"}

    filename_ext = ".skin.json"
    filter_glob: StringProperty(default="*.skin.json", options={"HIDDEN"})
    export_rig_definition: BoolProperty(
        name="Создать описание AnimationRig",
        description="Создать рядом файл *.animation-rig.es. Он нужен для анимаций полного TheRig-скелета.",
        default=True,
    )

    def draw(self, _context):
        self.layout.prop(self, "export_rig_definition")

    @classmethod
    def poll(cls, context):
        return bool(mesh_objects_from_context(context))

    def invoke(self, context, event):
        try:
            meshes = mesh_objects_from_context(context)
            if meshes and not self.filepath:
                armature = armature_for_mesh(meshes[0])
                schema = resolve_skeleton_schema(armature)
                leaf = schema.rig_resource_id.partition(":")[2].rstrip("/").split("/")[-1]
                if leaf:
                    self.filepath = leaf + self.filename_ext
        except RuntimeError:
            pass
        return ExportHelper.invoke(self, context, event)

    def execute(self, context):
        try:
            result = build_skin_model(mesh_objects_from_context(context))
            path = write_skin_json(self.filepath, result.model)
            rig_path = None
            if self.export_rig_definition and result.schema.schema_id == THERIG_SCHEMA_ID:
                rig_path = write_rig_definition(path, result.armature, result.schema)
            for warning in result.warnings:
                print("Echo skin export WARNING: " + warning)
            print("\n=== ECHO SKIN EXPORT COMPLETE ===")
            print(f"Rig: {result.schema.rig_resource_id}")
            print(f"Schema: {result.schema.schema_id}")
            print(f"Bones: {len(result.model['bones'])}")
            print(f"Meshes: {len(result.model['meshes'])}")
            print(f"Vertices: {result.vertex_count}")
            print(f"Triangles: {result.triangle_count}")
            print(path)
            if rig_path is not None:
                print(rig_path)
            suffix = f", предупреждений: {len(result.warnings)}" if result.warnings else ""
            self.report(
                {"INFO"},
                f"Экспортирован {Path(path).name}: костей {len(result.model['bones'])}, "
                f"вершин {result.vertex_count}"
                + (", AnimationRig создан" if rig_path is not None else "")
                + suffix,
            )
            return {"FINISHED"}
        except Exception as error:
            self.report({"ERROR"}, str(error))
            return {"CANCELLED"}


def draw_skin_export_menu(self, _context) -> None:
    self.layout.operator(
        ECHO_OT_export_skin.bl_idname,
        text="Модель Echo со скинингом (.skin.json)",
        icon="EXPORT",
    )


SKIN_EXPORT_CLASSES = (ECHO_OT_export_skin,)
