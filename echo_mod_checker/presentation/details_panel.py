from __future__ import annotations

import subprocess
from pathlib import Path

from PySide6.QtCore import Qt
from PySide6.QtWidgets import QFrame, QHBoxLayout, QLabel, QPushButton, QVBoxLayout

from echo_mod_checker.domain.models import ModResult
from echo_mod_checker.presentation.icon_button import IconButton


class DetailsPanel(QFrame):
    def __init__(self, project_dir: Path) -> None:
        super().__init__()
        self._project_dir = project_dir
        self._result: ModResult | None = None
        self.setObjectName("DetailsPanel")
        self.setFixedWidth(410)
        self.setVisible(False)
        self._build()

    def show_result(self, result: ModResult) -> None:
        self._result = result
        self._title.setText(result.name)
        self._body.setText(self._format(result))
        self._open_btn.setEnabled(result.jar_path is not None)
        self.setVisible(True)

    def _build(self) -> None:
        layout = QVBoxLayout(self)
        layout.setContentsMargins(18, 16, 18, 16)
        header = QHBoxLayout()
        self._title = QLabel("Подробности")
        self._title.setObjectName("title")
        close_btn = IconButton(self._project_dir, "x", "Закрыть подробности")
        close_btn.clicked.connect(self.hide)
        header.addWidget(self._title, 1)
        header.addWidget(close_btn)
        self._body = QLabel()
        self._body.setWordWrap(True)
        self._body.setTextInteractionFlags(Qt.TextSelectableByMouse)
        self._open_btn = QPushButton("Открыть расположение JAR")
        self._open_btn.clicked.connect(self._open_location)
        layout.addLayout(header)
        layout.addWidget(self._body, 1)
        layout.addWidget(self._open_btn)

    def _format(self, result: ModResult) -> str:
        values = [
            ("mod id", result.mod_id),
            ("Требуется", result.required_version),
            ("Установлено", result.installed_version),
            ("Loader", result.loader.value),
            ("JAR", result.jar_name),
            ("Путь", str(result.jar_path) if result.jar_path else None),
            ("Причина", result.reason),
        ]
        if result.duplicates:
            values.append(("Дубликаты", "\n".join(str(path) for path in result.duplicates)))
        return "\n\n".join(f"{key}: {value or '—'}" for key, value in values)

    def _open_location(self) -> None:
        if not self._result or not self._result.jar_path:
            return
        subprocess.Popen(["explorer", "/select,", str(self._result.jar_path)])
