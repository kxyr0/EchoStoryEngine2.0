from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from echo_mod_checker.domain.models import ExpectedMod
from echo_mod_checker.infrastructure.gradle_catalog import load_gradle_echo_mods


class ManifestError(Exception):
    pass


def read_manifest(path: Path) -> list[ExpectedMod]:
    try:
        raw = json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        raise ManifestError("manifest.json не найден") from exc
    except json.JSONDecodeError as exc:
        raise ManifestError(f"manifest.json содержит ошибку JSON: {exc.msg}") from exc
    except OSError as exc:
        raise ManifestError(f"Не удалось прочитать manifest.json: {exc}") from exc

    entries = _find_echo_mods(raw)
    if entries is None:
        raise ManifestError("В manifest.json нет секции echoMods со списком модов")
    mods = [_parse_entry(entry, index) for index, entry in enumerate(entries, start=1)]
    return load_gradle_echo_mods(path.resolve().parent.parent, mods)


def _find_echo_mods(raw: Any) -> list[Any] | None:
    if isinstance(raw, dict) and isinstance(raw.get("echoMods"), list):
        return raw["echoMods"]
    if isinstance(raw, dict):
        for value in raw.values():
            found = _find_echo_mods(value)
            if found is not None:
                return found
    if isinstance(raw, list):
        for value in raw:
            found = _find_echo_mods(value)
            if found is not None:
                return found
    return None


def _parse_entry(entry: Any, index: int) -> ExpectedMod:
    if not isinstance(entry, dict):
        raise ManifestError(f"Запись #{index} в echoMods должна быть объектом")
    mod_id = _required_string(entry, "id", index)
    version = _required_string(entry, "version", index)
    name = str(entry.get("name") or mod_id)
    required = bool(entry.get("required", True))
    patterns = entry.get("filePatterns", [])
    if patterns is None:
        patterns = []
    if not isinstance(patterns, list) or not all(isinstance(pattern, str) for pattern in patterns):
        raise ManifestError(f"filePatterns в записи #{index} должен быть списком строк")
    return ExpectedMod(
        mod_id=mod_id,
        name=name,
        version=version,
        required=required,
        file_patterns=tuple(patterns),
    )


def _required_string(entry: dict[str, Any], key: str, index: int) -> str:
    value = entry.get(key)
    if not isinstance(value, str) or not value.strip():
        raise ManifestError(f"В записи #{index} отсутствует поле {key}")
    return value.strip()
