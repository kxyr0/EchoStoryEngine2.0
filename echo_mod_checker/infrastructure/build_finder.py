from __future__ import annotations

from pathlib import Path

from echo_mod_checker.domain.models import ExpectedMod, JarMetadata
from echo_mod_checker.infrastructure.jar_scanner import read_jar_metadata

SKIPPED_SUFFIXES = ("-sources.jar", "-javadoc.jar", "-dev.jar")


def find_build_jar(mod: ExpectedMod, repo_root: Path) -> Path | None:
    matches: list[tuple[tuple[int, int, float, str], Path]] = []
    for jar_path in _build_jars(repo_root):
        metadata = _matching_metadata(jar_path, mod.mod_id)
        if metadata is None:
            continue
        exact_version = 0 if metadata.version == mod.version else 1
        all_jar = 0 if jar_path.name.endswith("-all.jar") else 1
        matches.append(((exact_version, all_jar, -jar_path.stat().st_mtime, jar_path.name.casefold()), jar_path))
    if not matches:
        return None
    return sorted(matches, key=lambda item: item[0])[0][1]


def _build_jars(repo_root: Path) -> list[Path]:
    mods_root = repo_root / "mods"
    if not mods_root.is_dir():
        return []
    jars = mods_root.glob("*/**/build/libs/*.jar")
    return sorted((path for path in jars if not path.name.endswith(SKIPPED_SUFFIXES)), key=lambda path: str(path).casefold())


def _matching_metadata(jar_path: Path, mod_id: str) -> JarMetadata | None:
    for metadata in read_jar_metadata(jar_path):
        if not metadata.metadata_error and metadata.mod_id == mod_id:
            return metadata
    return None
