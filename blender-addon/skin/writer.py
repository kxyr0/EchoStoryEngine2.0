from __future__ import annotations

import json
from pathlib import Path


def normalize_skin_path(filepath: str | Path) -> Path:
    path = Path(filepath)
    text = str(path)
    if not text.lower().endswith(".skin.json"):
        path = Path(text + ".skin.json")
    return path


def write_skin_json(filepath: str | Path, model: dict) -> Path:
    path = normalize_skin_path(filepath)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(model, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
        newline="\n",
    )
    return path
