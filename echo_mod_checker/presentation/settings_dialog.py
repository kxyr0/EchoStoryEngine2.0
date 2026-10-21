from __future__ import annotations

from pathlib import Path

from PySide6.QtWidgets import (
    QDialog,
    QFormLayout,
    QFileDialog,
    QHBoxLayout,
    QLineEdit,
    QPushButton,
    QVBoxLayout,
)

from echo_mod_checker.infrastructure.settings_store import CheckerSettings, SettingsStore


class SettingsDialog(QDialog):
    def __init__(self, store: SettingsStore, settings: CheckerSettings, parent=None) -> None:
        super().__init__(parent)
        self._store = store
        self._settings = settings
        self.setWindowTitle("Настройки")
        self._build()

    @property
    def settings(self) -> CheckerSettings:
        return self._settings

    def _build(self) -> None:
        root = QVBoxLayout(self)
        form = QFormLayout()
        self._mods = QLineEdit(str(self._settings.mods_path))
        self._manifest = QLineEdit(str(self._settings.manifest_path))
        form.addRow("Папка модов", self._mods)
        form.addRow("", self._button("Выбрать", self._pick_mods))
        form.addRow("manifest.json", self._manifest)
        form.addRow("", self._button("Выбрать", self._pick_manifest))
        actions = QHBoxLayout()
        actions.addWidget(self._button("Сбросить пути", self._reset))
        actions.addStretch(1)
        actions.addWidget(self._button("Готово", self._accept_current))
        root.addLayout(form)
        root.addLayout(actions)

    def _button(self, text: str, slot) -> QPushButton:
        button = QPushButton(text)
        button.clicked.connect(slot)
        return button

    def _pick_mods(self) -> None:
        chosen = QFileDialog.getExistingDirectory(self, "Выбрать папку с модами", self._mods.text())
        if chosen:
            self._mods.setText(chosen)

    def _pick_manifest(self) -> None:
        chosen, _ = QFileDialog.getOpenFileName(self, "Выбрать manifest.json", self._manifest.text(), "JSON (*.json)")
        if chosen:
            self._manifest.setText(chosen)

    def _reset(self) -> None:
        self._settings = self._store.reset()
        self.accept()

    def _accept_current(self) -> None:
        self._settings = CheckerSettings(Path(self._mods.text()), Path(self._manifest.text()))
        self._store.save(self._settings)
        self.accept()
