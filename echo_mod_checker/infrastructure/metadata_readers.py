from __future__ import annotations

import json
import tomllib
import zipfile
from email.parser import Parser
from pathlib import Path
from typing import Any

from echo_mod_checker.domain.models import JarMetadata
from echo_mod_checker.domain.statuses import ModLoader


class FabricMetadataReader:
    metadata_name = "fabric.mod.json"

    def has_metadata(self, names: set[str]) -> bool:
        return self.metadata_name in names

    def read(self, archive: zipfile.ZipFile, jar_path: Path) -> list[JarMetadata]:
        try:
            raw = json.loads(archive.read(self.metadata_name).decode("utf-8"))
        except UnicodeDecodeError:
            return [_error(jar_path, ModLoader.FABRIC, "fabric.mod.json не в UTF-8")]
        except json.JSONDecodeError:
            return [_error(jar_path, ModLoader.FABRIC, "fabric.mod.json содержит ошибку JSON")]
        if not isinstance(raw, dict):
            return [_error(jar_path, ModLoader.FABRIC, "fabric.mod.json должен быть объектом")]
        mod_id = _clean(raw.get("id"))
        version = _clean(raw.get("version"))
        name = _clean(raw.get("name")) or mod_id
        if not mod_id:
            return [JarMetadata(jar_path, name=name, version=version, loader=ModLoader.FABRIC, metadata_error="id отсутствует")]
        if not version:
            return [JarMetadata(jar_path, mod_id=mod_id, name=name, loader=ModLoader.FABRIC, metadata_error="version отсутствует")]
        return [JarMetadata(jar_path, mod_id=mod_id, name=name, version=version, loader=ModLoader.FABRIC)]


class ForgeMetadataReader:
    metadata_name = "META-INF/mods.toml"

    def has_metadata(self, names: set[str]) -> bool:
        return self.metadata_name in names

    def read(self, archive: zipfile.ZipFile, jar_path: Path) -> list[JarMetadata]:
        try:
            raw = tomllib.loads(archive.read(self.metadata_name).decode("utf-8"))
        except UnicodeDecodeError:
            return [_error(jar_path, ModLoader.FORGE, "mods.toml не в UTF-8")]
        except tomllib.TOMLDecodeError:
            return [_error(jar_path, ModLoader.FORGE, "mods.toml содержит ошибку TOML")]
        mods = raw.get("mods")
        if not isinstance(mods, list) or not mods:
            return [_error(jar_path, ModLoader.FORGE, "В mods.toml нет [[mods]]")]
        return [self._read_mod(jar_path, section, archive) for section in mods]

    def _read_mod(self, jar_path: Path, section: Any, archive: zipfile.ZipFile) -> JarMetadata:
        if not isinstance(section, dict):
            return _error(jar_path, ModLoader.FORGE, "Секция [[mods]] должна быть объектом")
        mod_id = _clean(section.get("modId"))
        name = _clean(section.get("displayName")) or mod_id
        version = _clean(section.get("version"))
        if version == "${file.jarVersion}":
            version = _manifest_version(archive)
        if not mod_id:
            return JarMetadata(jar_path, name=name, version=version, loader=ModLoader.FORGE, metadata_error="modId отсутствует")
        if not version:
            return JarMetadata(jar_path, mod_id=mod_id, name=name, loader=ModLoader.FORGE, metadata_error="version отсутствует")
        return JarMetadata(jar_path, mod_id=mod_id, name=name, version=version, loader=ModLoader.FORGE)


def _manifest_version(archive: zipfile.ZipFile) -> str | None:
    try:
        raw = archive.read("META-INF/MANIFEST.MF").decode("utf-8", errors="replace")
    except KeyError:
        return None
    return Parser().parsestr(raw).get("Implementation-Version")


def _error(jar_path: Path, loader: ModLoader, message: str) -> JarMetadata:
    return JarMetadata(jar_path, loader=loader, metadata_error=message)


def _clean(value: object) -> str | None:
    if isinstance(value, str) and value.strip():
        return value.strip()
    return None
