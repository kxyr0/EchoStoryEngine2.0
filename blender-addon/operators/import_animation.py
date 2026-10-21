from __future__ import annotations

from ..animation.importer import ECHO_OT_import_animation_es


def draw_animation_import_menu(self, _context) -> None:
    self.layout.operator(
        ECHO_OT_import_animation_es.bl_idname,
        text="Анимация Echo (.es)",
        icon="IMPORT",
    )
