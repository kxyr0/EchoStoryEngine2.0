from __future__ import annotations

import logging
import traceback
from pathlib import Path

from PySide6.QtCore import QObject, Signal, Slot

from echo_mod_checker.application.check_service import run_check
from echo_mod_checker.domain.models import CheckSummary

LOGGER = logging.getLogger(__name__)


class CheckWorker(QObject):
    stage_changed = Signal(str)
    progress_changed = Signal(int, int)
    current_file_changed = Signal(str)
    completed = Signal(object)
    failed = Signal(str)

    def __init__(self, mods_path: Path, manifest_path: Path) -> None:
        super().__init__()
        self._mods_path = mods_path
        self._manifest_path = manifest_path

    @Slot()
    def run(self) -> None:
        try:
            summary = run_check(
                self._mods_path,
                self._manifest_path,
                self.stage_changed.emit,
                self._on_progress,
            )
            LOGGER.info("Check completed: %s issue(s)", summary.issue_count)
            self.completed.emit(summary)
        except Exception as exc:
            LOGGER.error("Check failed: %s\n%s", exc, traceback.format_exc())
            self.failed.emit(str(exc) or "Проверку не удалось выполнить")

    def _on_progress(self, current: int, total: int, jar_path: Path) -> None:
        self.progress_changed.emit(current, total)
        self.current_file_changed.emit(jar_path.name)
