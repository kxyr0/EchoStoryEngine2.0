from __future__ import annotations

from dataclasses import dataclass
import logging
import shutil
from pathlib import Path

from echo_mod_checker.domain.models import CheckSummary, ExpectedMod, ModResult
from echo_mod_checker.domain.statuses import ModStatus
from echo_mod_checker.infrastructure.build_finder import find_build_jar

LOGGER = logging.getLogger(__name__)
MISSING_STATUSES = {ModStatus.MISSING_REQUIRED, ModStatus.MISSING_OPTIONAL}


@dataclass(frozen=True)
class InstallItem:
    result: ModResult
    source: Path | None
    destination: Path | None
    installed: bool
    reason: str


@dataclass(frozen=True)
class InstallReport:
    items: tuple[InstallItem, ...]

    @property
    def installed_count(self) -> int:
        return sum(1 for item in self.items if item.installed)

    @property
    def missing_count(self) -> int:
        return sum(1 for item in self.items if item.source is None)


def install_missing_mods(summary: CheckSummary, repo_root: Path) -> InstallReport:
    summary.mods_path.mkdir(parents=True, exist_ok=True)
    items = []
    for result in summary.results:
        if result.status not in MISSING_STATUSES or not result.mod_id or not result.required_version:
            continue
        expected = ExpectedMod(result.mod_id, result.name, result.required_version, result.required)
        source = find_build_jar(expected, repo_root)
        if source is None:
            items.append(InstallItem(result, None, None, False, "JAR не найден в mods/**/build/libs."))
            continue
        destination = summary.mods_path / source.name
        if destination.exists():
            items.append(InstallItem(result, source, destination, False, "Файл с таким именем уже есть в mods."))
            continue
        shutil.copy2(source, destination)
        LOGGER.info("Installed missing Echo mod: %s -> %s", source, destination)
        items.append(InstallItem(result, source, destination, True, "Установлен."))
    return InstallReport(tuple(items))
