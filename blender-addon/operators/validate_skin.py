from __future__ import annotations

import bpy

from ..skin.exporter import build_skin_model, mesh_objects_from_context


class ECHO_OT_validate_skin(bpy.types.Operator):
    bl_idname = "echo.validate_skin"
    bl_label = "Проверить модель Echo"
    bl_description = "Проверить скелет, bind-матрицы, UV, треугольники и веса скининга без создания файла. Используйте перед экспортом модели."
    bl_options = {"REGISTER"}

    @classmethod
    def poll(cls, context):
        return bool(mesh_objects_from_context(context))

    def execute(self, context):
        try:
            result = build_skin_model(mesh_objects_from_context(context))
            for warning in result.warnings:
                print("Echo skin validation WARNING: " + warning)
            self.report(
                {"INFO"},
                f"Модель корректна: костей {len(result.model['bones'])}, "
                f"вершин {result.vertex_count}, треугольников {result.triangle_count}, "
                f"предупреждений {len(result.warnings)}",
            )
            return {"FINISHED"}
        except Exception as error:
            self.report({"ERROR"}, str(error))
            return {"CANCELLED"}


SKIN_VALIDATION_CLASSES = (ECHO_OT_validate_skin,)
