from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

from PySide6.QtCore import QSettings

DEFAULT_MODS_PATH = Path(
    r"C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\mods"
)


@dataclass(frozen=True)
class CheckerSettings:
    mods_path: Path
    manifest_path: Path


class SettingsStore:
    def __init__(self, project_dir: Path) -> None:
        self._project_dir = project_dir
        self._settings = QSettings("Echo Platform", "Echo Mod Checker")

    def load(self) -> CheckerSettings:
        manifest_default = self._project_dir / "manifest.json"
        mods_path = self._settings.value("mods_path", str(DEFAULT_MODS_PATH))
        manifest_path = self._settings.value("manifest_path", str(manifest_default))
        return CheckerSettings(Path(str(mods_path)), Path(str(manifest_path)))

    def save(self, settings: CheckerSettings) -> None:
        self._settings.setValue("mods_path", str(settings.mods_path))
        self._settings.setValue("manifest_path", str(settings.manifest_path))

    def reset(self) -> CheckerSettings:
        self._settings.remove("mods_path")
        self._settings.remove("manifest_path")
        settings = self.load()
        self.save(settings)
        return settings
