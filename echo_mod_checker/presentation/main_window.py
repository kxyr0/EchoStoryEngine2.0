from __future__ import annotations

import logging
import subprocess
from pathlib import Path

from PySide6.QtCore import QSize, Qt, QThread
from PySide6.QtWidgets import QFileDialog, QFrame, QHBoxLayout, QLabel, QMainWindow, QProgressBar, QPushButton, QSizePolicy, QVBoxLayout, QWidget

from echo_mod_checker.application.check_worker import CheckWorker
from echo_mod_checker.application.install_service import install_missing_mods
from echo_mod_checker.domain.models import CheckSummary
from echo_mod_checker.infrastructure.settings_store import CheckerSettings, SettingsStore
from echo_mod_checker.presentation.animations import fade_in
from echo_mod_checker.presentation.details_panel import DetailsPanel
from echo_mod_checker.presentation.icon_button import FooterBar, HeaderBar, icon
from echo_mod_checker.presentation.result_view import ResultList, has_missing_mods, has_only_success, summary_text
from echo_mod_checker.presentation.settings_dialog import SettingsDialog
from echo_mod_checker.presentation.styles import COLORS, app_stylesheet

LOGGER = logging.getLogger(__name__)


class MainWindow(QMainWindow):
    def __init__(self, project_dir: Path) -> None:
        super().__init__()
        self._project_dir = project_dir
        self._store = SettingsStore(project_dir)
        self._settings = self._store.load()
        self._thread: QThread | None = None
        self._worker: CheckWorker | None = None
        self._animations = []
        self._last_results = []
        self._last_summary: CheckSummary | None = None
        self._build()
        self._set_idle()

    def _build(self) -> None:
        self.setWindowTitle("Echo Mod Checker")
        self.setMinimumSize(900, 620)
        self.setStyleSheet(app_stylesheet())
        root = QWidget()
        root.setObjectName("Root")
        shell = QHBoxLayout(root)
        shell.setContentsMargins(28, 28, 28, 28)
        shell.setSpacing(0)
        content = QWidget()
        content.setMaximumWidth(1500)
        content.setSizePolicy(QSizePolicy.Expanding, QSizePolicy.Expanding)
        content_row = QHBoxLayout(content)
        content_row.setContentsMargins(0, 0, 0, 0)
        content_row.setSpacing(16)
        left = QWidget()
        layout = QVBoxLayout(left)
        layout.setContentsMargins(0, 0, 0, 0)
        layout.setSpacing(14)
        layout.addWidget(HeaderBar(self._project_dir, self._choose_mods, self._show_settings, self._open_log))
        status_card = QFrame()
        status_card.setObjectName("StatusCard")
        status_card.setMinimumHeight(170)
        status_card.setMaximumHeight(260)
        status_layout = QVBoxLayout(status_card)
        status_layout.setContentsMargins(24, 22, 24, 22)
        status_layout.setSpacing(10)
        self._state = QLabel()
        self._state.setObjectName("state")
        self._state.setAlignment(Qt.AlignCenter)
        self._subtitle = QLabel()
        self._subtitle.setObjectName("subtitle")
        self._subtitle.setAlignment(Qt.AlignCenter)
        self._subtitle.setWordWrap(True)
        self._progress = QProgressBar()
        self._progress.setVisible(False)
        self._current = QLabel()
        self._current.setObjectName("muted")
        self._current.setAlignment(Qt.AlignCenter)
        self._primary = QPushButton("Проверить моды")
        self._primary.setObjectName("primary")
        self._primary.setIcon(icon(self._project_dir, "scan"))
        self._primary.setIconSize(QSize(18, 18))
        self._primary.setMinimumWidth(280)
        self._primary.setToolTip("Проверить моды")
        self._primary.clicked.connect(self._start_check)
        self._summary = QLabel()
        self._summary.setObjectName("muted")
        self._summary.setAlignment(Qt.AlignCenter)
        self._show_all = QPushButton("Показать все моды")
        self._show_all.clicked.connect(self._show_all_results)
        self._install = QPushButton("Установить недостающие")
        self._install.setObjectName("secondary")
        self._install.clicked.connect(self._install_missing)
        status_layout.addWidget(self._state)
        status_layout.addWidget(self._subtitle)
        status_layout.addWidget(self._progress)
        status_layout.addWidget(self._current)
        status_layout.addWidget(self._primary, 0, Qt.AlignCenter)
        status_layout.addWidget(self._install, 0, Qt.AlignCenter)
        status_layout.addWidget(self._summary)
        status_layout.addWidget(self._show_all, 0, Qt.AlignCenter)
        problems = QFrame()
        problems.setObjectName("ProblemsPanel")
        problems_layout = QVBoxLayout(problems)
        problems_layout.setContentsMargins(16, 14, 16, 14)
        problems_layout.setSpacing(8)
        self._empty = QLabel()
        self._empty.setObjectName("muted")
        self._empty.setAlignment(Qt.AlignCenter)
        self._results = ResultList(self._project_dir)
        problems_layout.addWidget(self._empty, 1)
        problems_layout.addWidget(self._results, 1)
        layout.addWidget(status_card)
        layout.addWidget(problems, 1)
        self._footer = FooterBar(self._settings.mods_path)
        layout.addWidget(self._footer)
        self._details = DetailsPanel(self._project_dir)
        self._results.result_selected.connect(self._show_details)
        content_row.addWidget(left, 1)
        content_row.addWidget(self._details)
        shell.addStretch(1)
        shell.addWidget(content, 100)
        shell.addStretch(1)
        self.setCentralWidget(root)

    def _set_idle(self) -> None:
        self._state.setText("Готово к проверке")
        self._subtitle.setText("Нажмите одну кнопку, чтобы проверить Echo-моды в сборке.")
        self._summary.setText("")
        self._current.setText("")
        self._progress.setVisible(False)
        self._results.setVisible(False)
        self._empty.setText("Проверка ещё не выполнялась."); self._empty.setVisible(True)
        self._show_all.setVisible(False)
        self._install.setVisible(False)

    def _start_check(self) -> None:
        LOGGER.info("Selected mods=%s manifest=%s", self._settings.mods_path, self._settings.manifest_path)
        self._primary.setEnabled(False)
        self._state.setText("Проверяем моды")
        self._state.setStyleSheet("")
        self._subtitle.setText("Читаем метаданные Forge/Fabric и сравниваем Echo-моды.")
        self._summary.setText("")
        self._current.setText("")
        self._progress.setValue(0)
        self._progress.setVisible(True)
        self._results.setVisible(False)
        self._empty.setText("Сканирование выполняется в фоне."); self._empty.setVisible(True)
        self._show_all.setVisible(False)
        self._install.setVisible(False)
        self._details.hide()
        self._thread = QThread(self)
        self._worker = CheckWorker(self._settings.mods_path, self._settings.manifest_path)
        self._worker.moveToThread(self._thread)
        self._thread.started.connect(self._worker.run)
        self._worker.stage_changed.connect(self._subtitle.setText)
        self._worker.progress_changed.connect(self._set_progress)
        self._worker.current_file_changed.connect(self._current.setText)
        self._worker.completed.connect(self._complete_check)
        self._worker.failed.connect(self._fail_check)
        self._worker.completed.connect(self._thread.quit)
        self._worker.failed.connect(self._thread.quit)
        self._thread.finished.connect(self._thread.deleteLater)
        self._thread.start()

    def _set_progress(self, current: int, total: int) -> None:
        self._progress.setMaximum(max(total, 1))
        self._progress.setValue(current)
        self._summary.setText(f"{current} из {total}")

    def _complete_check(self, summary: CheckSummary) -> None:
        self._primary.setEnabled(True)
        self._last_summary = summary
        self._progress.setVisible(False)
        self._current.setText("")
        if summary.critical_count:
            self._state.setStyleSheet(f"color: {COLORS['error']};")
            self._state.setText(f"Найдены {summary.issue_count} проблемы")
            self._subtitle.setText("Есть ошибки, которые лучше исправить перед запуском.")
        elif summary.warning_count:
            self._state.setStyleSheet(f"color: {COLORS['warning']};")
            self._state.setText(f"Найдены {summary.issue_count} предупреждения")
            self._subtitle.setText("Сборка запускается не в ожидаемом составе.")
        else:
            self._state.setStyleSheet(f"color: {COLORS['success']};")
            self._state.setText("Echo-моды актуальны")
            self._subtitle.setText("Обязательные Echo-моды совпадают с ожидаемым списком.")
        self._summary.setText(summary_text(summary))
        self._last_results = list(summary.results)
        only_success = has_only_success(self._last_results)
        self._results.set_results(self._last_results, show_ok=not only_success)
        self._empty.setText("Проблемы не найдены.")
        self._empty.setVisible(not any(result.status.is_problem for result in self._last_results))
        self._show_all.setVisible(only_success and bool(self._last_results))
        self._install.setVisible(has_missing_mods(summary))
        self._animations.append(fade_in(self._results))

    def _fail_check(self, message: str) -> None:
        self._primary.setEnabled(True)
        self._progress.setVisible(False)
        self._state.setStyleSheet(f"color: {COLORS['error']};")
        self._state.setText("Не удалось выполнить проверку")
        self._subtitle.setText(message)
        self._summary.setText("Подробности записаны в журнал.")
        self._empty.setText("Проверка не завершилась.")
        self._empty.setVisible(True)
        self._install.setVisible(False)

    def _install_missing(self) -> None:
        if not self._last_summary:
            return
        self._install.setEnabled(False)
        try:
            report = install_missing_mods(self._last_summary, self._project_dir.parent)
        except OSError as exc:
            LOGGER.exception("Failed to install missing mods")
            self._subtitle.setText(f"Не удалось установить моды: {exc}")
            self._install.setEnabled(True)
            return
        self._subtitle.setText(f"Установлено: {report.installed_count}; не найдено в build/libs: {report.missing_count}.")
        self._install.setEnabled(True)
        if report.installed_count:
            self._start_check()

    def _choose_mods(self) -> None:
        chosen = QFileDialog.getExistingDirectory(self, "Выбрать папку с модами", str(self._settings.mods_path))
        if chosen:
            self._settings = CheckerSettings(Path(chosen), self._settings.manifest_path)
            self._store.save(self._settings)
            self._footer.set_path(self._settings.mods_path)

    def _show_settings(self) -> None:
        dialog = SettingsDialog(self._store, self._settings, self)
        if dialog.exec():
            self._settings = dialog.settings
            self._footer.set_path(self._settings.mods_path)

    def _open_log(self) -> None:
        subprocess.Popen(["explorer", str(self._project_dir / "logs")])

    def _show_details(self, result: object) -> None:
        self._details.show_result(result)
        self._animations.append(fade_in(self._details))

    def _show_all_results(self) -> None:
        self._results.set_results(self._last_results, show_ok=True)
        self._empty.setVisible(False)
        self._show_all.setVisible(False)
        self._animations.append(fade_in(self._results))
