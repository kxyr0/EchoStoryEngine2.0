from __future__ import annotations

from pathlib import Path

from echo_mod_checker.domain.models import ExpectedMod


def load_gradle_echo_mods(repo_root: Path, existing: list[ExpectedMod]) -> list[ExpectedMod]:
    mods_root = repo_root / "mods"
    if not mods_root.is_dir():
        return existing

    existing_by_id = {mod.mod_id: mod for mod in existing}
    discovered: dict[str, ExpectedMod] = {}
    for properties_path in sorted(mods_root.glob("*/gradle.properties"), key=lambda path: str(path).casefold()):
        properties = _read_properties(properties_path)
        mod_id = properties.get("mod_id", "").strip()
        version = properties.get("mod_version", "").strip()
        if not _is_echo_id(mod_id) or not version:
            continue
        previous = existing_by_id.get(mod_id)
        raw_name = properties.get("mod_name", "").strip()
        name = raw_name or (previous.name if previous else mod_id)
        artifact = properties.get("artifact_name", "").strip() or properties_path.parent.name
        discovered[mod_id] = ExpectedMod(
            mod_id=mod_id,
            name=name,
            version=version,
            required=previous.required if previous else _default_required(mod_id, name),
            file_patterns=previous.file_patterns if previous and previous.file_patterns else (f"{artifact}-*.jar",),
        )

    merged: list[ExpectedMod] = []
    seen: set[str] = set()
    for mod in existing:
        merged.append(discovered.get(mod.mod_id, mod))
        seen.add(mod.mod_id)
    merged.extend(discovered[mod_id] for mod_id in sorted(discovered) if mod_id not in seen)
    return merged


def _read_properties(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith(("#", "!")):
            continue
        separator = "=" if "=" in stripped else ":" if ":" in stripped else None
        if separator:
            key, value = stripped.split(separator, 1)
            values[key.strip()] = value.strip()
    return values


def _is_echo_id(mod_id: str) -> bool:
    folded = mod_id.casefold()
    return folded.startswith("echo_") or folded.startswith("echo-")


def _default_required(mod_id: str, name: str) -> bool:
    folded = f"{mod_id} {name}".casefold()
    return "debug" not in folded and "authoring" not in folded
