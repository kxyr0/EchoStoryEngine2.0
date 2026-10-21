from __future__ import annotations

import logging
import zipfile
from pathlib import Path
from typing import Callable

from echo_mod_checker.domain.models import JarMetadata
from echo_mod_checker.infrastructure.metadata_readers import FabricMetadataReader, ForgeMetadataReader

ProgressCallback = Callable[[int, int, Path], None]
LOGGER = logging.getLogger(__name__)
READERS = (FabricMetadataReader(), ForgeMetadataReader())


def list_jars(mods_path: Path) -> list[Path]:
    if not mods_path.exists():
        raise FileNotFoundError("Папка с модами не найдена")
    if not mods_path.is_dir():
        raise NotADirectoryError("Путь к модам не является папкой")
    return sorted(mods_path.glob("*.jar"), key=lambda path: path.name.casefold())


def scan_jars(
    mods_path: Path,
    progress: ProgressCallback | None = None,
) -> list[JarMetadata]:
    jars = list_jars(mods_path)
    scanned: list[JarMetadata] = []
    total = len(jars)
    for index, jar_path in enumerate(jars, start=1):
        if progress:
            progress(index, total, jar_path)
        scanned.extend(read_jar_metadata(jar_path))
    return scanned


def read_jar_metadata(jar_path: Path) -> list[JarMetadata]:
    try:
        with zipfile.ZipFile(jar_path) as archive:
            names = set(archive.namelist())
            entries: list[JarMetadata] = []
            for reader in READERS:
                if reader.has_metadata(names):
                    entries.extend(reader.read(archive, jar_path))
            if entries:
                return entries
            LOGGER.debug("Unrecognized mod JAR: %s", jar_path)
            return [JarMetadata(jar_path, metadata_error="Формат мода не распознан", unrecognized=True)]
    except zipfile.BadZipFile:
        LOGGER.warning("Bad JAR archive: %s", jar_path)
        return [JarMetadata(jar_path, metadata_error="JAR повреждён", corrupted=True)]
    except OSError as exc:
        LOGGER.warning("JAR access error: %s: %s", jar_path, exc)
        return [JarMetadata(jar_path, metadata_error=f"Ошибка доступа к JAR: {exc}", corrupted=True)]
