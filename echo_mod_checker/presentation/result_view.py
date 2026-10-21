from __future__ import annotations

from pathlib import Path

from PySide6.QtCore import QSize, Signal
from PySide6.QtWidgets import QHBoxLayout, QLabel, QListWidget, QListWidgetItem, QWidget

from echo_mod_checker.domain.models import CheckSummary, ModResult
from echo_mod_checker.domain.statuses import ModStatus
from echo_mod_checker.presentation.icon_button import icon
from echo_mod_checker.presentation.styles import COLORS

MISSING_STATUSES = {ModStatus.MISSING_REQUIRED, ModStatus.MISSING_OPTIONAL}


class ResultList(QListWidget):
    result_selected = Signal(object)

    def __init__(self, project_dir: Path) -> None:
        super().__init__()
        self._project_dir = project_dir
        self._results: list[ModResult] = []
        self.itemSelectionChanged.connect(self._emit_selection)

    def set_results(self, results: list[ModResult], show_ok: bool = False) -> None:
        self.clear()
        self._results = [r for r in results if show_ok or r.status.is_problem]
        for result in self._results:
            item = QListWidgetItem()
            item.setSizeHint(QSize(0, 44))
            self.addItem(item)
            self.setItemWidget(item, self._row(result))
        self.setVisible(bool(self._results))

    def _emit_selection(self) -> None:
        selected = self.currentRow()
        if 0 <= selected < len(self._results):
            self.result_selected.emit(self._results[selected])

    def _row(self, result: ModResult) -> QWidget:
        row = QWidget()
        row.setObjectName("ResultRow")
        layout = QHBoxLayout(row)
        layout.setContentsMargins(8, 4, 8, 4)
        status = QLabel()
        status.setPixmap(icon(self._project_dir, result.status.icon_name).pixmap(18, 18))
        name = QLabel(result.name)
        version = QLabel(result.installed_version or "—")
        reason = QLabel(result.status.label if result.status is ModStatus.OK else result.reason or result.status.label)
        name.setMinimumWidth(220)
        name.setStyleSheet(f"color: {_name_color(result)}; font-weight: 600;")
        version.setMinimumWidth(90)
        reason.setObjectName("muted")
        layout.addWidget(status)
        layout.addWidget(name, 2)
        layout.addWidget(version)
        layout.addWidget(reason, 3)
        return row

    def _label(self, result: ModResult) -> str:
        version = result.installed_version or "—"
        return f"{result.name}    {version}    {result.status.label}"


def has_only_success(results: list[ModResult]) -> bool:
    return not any(result.status.is_problem for result in results)


def has_missing_mods(summary: CheckSummary) -> bool:
    return any(result.status in MISSING_STATUSES for result in summary.results)


def _name_color(result: ModResult) -> str:
    if result.status in MISSING_STATUSES:
        return COLORS["error"]
    if result.status in {ModStatus.BAD_JAR, ModStatus.BAD_METADATA, ModStatus.UNRECOGNIZED, ModStatus.INVALID_VERSION}:
        return COLORS["error"]
    return COLORS["success"] if result.jar_path else COLORS["text"]


def summary_text(summary: CheckSummary) -> str:
    if not summary.has_issues:
        return f"{summary.checked_count} модов проверено"
    parts = []
    labels = (
        (ModStatus.OUTDATED, "устарели"),
        (ModStatus.MISSING_REQUIRED, "отсутствуют"),
        (ModStatus.DUPLICATE, "дубликаты"),
        (ModStatus.BAD_JAR, "повреждены"),
        (ModStatus.BAD_METADATA, "с ошибками метаданных"),
        (ModStatus.UNRECOGNIZED, "не распознаны"),
        (ModStatus.INVALID_VERSION, "с некорректной версией"),
    )
    for status, label in labels:
        count = summary.count(status)
        if count:
            parts.append(f"{count} {label}")
    return "   ".join(parts)
