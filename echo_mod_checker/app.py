from __future__ import annotations

import sys
from pathlib import Path


def _ensure_package_parent() -> None:
    if __package__:
        return
    package_parent = Path(__file__).resolve().parent.parent
    if str(package_parent) not in sys.path:
        sys.path.insert(0, str(package_parent))


def main() -> int:
    _ensure_package_parent()
    from PySide6.QtWidgets import QApplication

    from echo_mod_checker.infrastructure.logging_setup import configure_logging
    from echo_mod_checker.presentation.main_window import MainWindow

    configure_logging(Path(__file__).resolve().parent)
    app = QApplication(sys.argv)
    app.setApplicationName("Echo Mod Checker")
    app.setOrganizationName("Echo Platform")
    window = MainWindow(Path(__file__).resolve().parent)
    window.show()
    return app.exec()


if __name__ == "__main__":
    raise SystemExit(main())
