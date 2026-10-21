from __future__ import annotations

from pathlib import Path
from typing import Callable

from PySide6.QtCore import QSize, Qt
from PySide6.QtGui import QIcon
from PySide6.QtWidgets import QFrame, QHBoxLayout, QLabel, QPushButton


def icon_path(project_dir: Path, name: str) -> Path:
    return project_dir / "assets" / "icons" / f"{name}.svg"


def icon(project_dir: Path, name: str) -> QIcon:
    return QIcon(str(icon_path(project_dir, name)))


class IconButton(QPushButton):
    def __init__(self, project_dir: Path, icon_name: str, tooltip: str) -> None:
        super().__init__()
        self.setObjectName("icon")
        self.setIcon(icon(project_dir, icon_name))
        self.setIconSize(QSize(20, 20))
        self.setFixedSize(64, 52)
        self.setToolTip(tooltip)
        self.setFocusPolicy(Qt.NoFocus)


class HeaderBar(QFrame):
    def __init__(
        self,
        project_dir: Path,
        on_choose_mods: Callable[[], None],
        on_settings: Callable[[], None],
        on_log: Callable[[], None],
    ) -> None:
        super().__init__()
        self.setObjectName("Header")
        self.setMinimumHeight(56)
        self.setMaximumHeight(64)
        row = QHBoxLayout(self)
        row.setContentsMargins(14, 6, 14, 6)
        folder = IconButton(project_dir, "folder", "Выбрать папку с модами")
        settings = IconButton(project_dir, "settings", "Настройки")
        log = IconButton(project_dir, "log", "Открыть журнал")
        folder.clicked.connect(on_choose_mods)
        settings.clicked.connect(on_settings)
        log.clicked.connect(on_log)
        row.addStretch(1)
        row.addWidget(folder)
        row.addWidget(settings)
        row.addWidget(log)
        row.addStretch(1)


class FooterBar(QFrame):
    def __init__(self, mods_path: Path) -> None:
        super().__init__()
        self.setObjectName("Footer")
        self.setMaximumHeight(52)
        row = QHBoxLayout(self)
        row.setContentsMargins(16, 8, 16, 8)
        self._path = QLabel()
        self._path.setObjectName("path")
        row.addWidget(self._path, 1)
        self.set_path(mods_path)

    def set_path(self, path: Path) -> None:
        self._path.setText(path.parent.parent.name + " / " + path.parent.name + " / " + path.name)
