from __future__ import annotations

from enum import Enum

import re

from packaging.version import InvalidVersion, Version


class VersionRelation(Enum):
    EQUAL = "equal"
    OUTDATED = "outdated"
    NEWER = "newer"
    INVALID = "invalid"


def compare_versions(installed: str, required: str) -> VersionRelation:
    try:
        installed_version = Version(_normalize(installed))
        required_version = Version(_normalize(required))
    except InvalidVersion:
        return VersionRelation.INVALID
    if installed_version == required_version:
        return VersionRelation.EQUAL
    if installed_version < required_version:
        return VersionRelation.OUTDATED
    return VersionRelation.NEWER


def _normalize(value: str) -> str:
    cleaned = value.strip()
    snapshot = re.fullmatch(r"(.+)-SNAPSHOT", cleaned, flags=re.IGNORECASE)
    if snapshot:
        return f"{snapshot.group(1)}.dev0"
    local = re.fullmatch(r"([0-9][0-9A-Za-z.!]*?)-([A-Za-z][0-9A-Za-z.-]*)", cleaned)
    if local:
        suffix = re.sub(r"[^0-9A-Za-z.]+", ".", local.group(2)).strip(".")
        return f"{local.group(1)}+{suffix.lower()}"
    return cleaned
