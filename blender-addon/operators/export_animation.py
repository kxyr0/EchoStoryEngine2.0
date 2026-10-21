from __future__ import annotations

from ..animation.exporter import ECHO_OT_export_animation


def draw_animation_export_menu(self, _context) -> None:
    self.layout.operator(
        ECHO_OT_export_animation.bl_idname,
        text="Анимация Echo (.es)",
        icon="EXPORT",
    )
