from __future__ import annotations

import logging
from pathlib import Path


def configure_logging(project_dir: Path) -> Path:
    log_dir = project_dir / "logs"
    log_dir.mkdir(parents=True, exist_ok=True)
    log_path = log_dir / "echo-mod-checker.log"
    logging.basicConfig(
        filename=log_path,
        level=logging.INFO,
        encoding="utf-8",
        format="%(asctime)s %(levelname)s %(name)s: %(message)s",
    )
    logging.info("Echo Mod Checker started")
    return log_path
