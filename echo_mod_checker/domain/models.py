from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path

from .statuses import ModLoader, ModStatus, Severity


@dataclass(frozen=True)
class ExpectedMod:
    mod_id: str
    name: str
    version: str
    required: bool = True
    file_patterns: tuple[str, ...] = field(default_factory=tuple)


@dataclass(frozen=True)
class JarMetadata:
    jar_path: Path
    mod_id: str | None = None
    name: str | None = None
    version: str | None = None
    loader: ModLoader = ModLoader.UNKNOWN
    metadata_error: str | None = None
    corrupted: bool = False
    unrecognized: bool = False

    @property
    def jar_name(self) -> str:
        return self.jar_path.name


@dataclass(frozen=True)
class ModResult:
    status: ModStatus
    name: str
    mod_id: str | None = None
    required_version: str | None = None
    installed_version: str | None = None
    loader: ModLoader = ModLoader.UNKNOWN
    jar_path: Path | None = None
    reason: str = ""
    required: bool = False
    duplicates: tuple[Path, ...] = field(default_factory=tuple)

    @property
    def severity(self) -> Severity:
        return self.status.severity

    @property
    def jar_name(self) -> str:
        return self.jar_path.name if self.jar_path else "—"


@dataclass(frozen=True)
class CheckSummary:
    mods_path: Path
    manifest_path: Path
    jar_count: int
    results: tuple[ModResult, ...]

    def count(self, status: ModStatus) -> int:
        return sum(1 for result in self.results if result.status is status)

    @property
    def checked_count(self) -> int:
        return self.count(ModStatus.OK)

    @property
    def warning_count(self) -> int:
        return sum(1 for result in self.results if result.status.is_problem and result.severity is Severity.WARNING)

    @property
    def critical_count(self) -> int:
        return sum(1 for result in self.results if result.status.is_problem and result.severity is Severity.CRITICAL)

    @property
    def issue_count(self) -> int:
        return sum(1 for result in self.results if result.status.is_problem)

    @property
    def has_issues(self) -> bool:
        return self.issue_count > 0
