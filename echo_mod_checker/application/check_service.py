from __future__ import annotations

from collections import defaultdict
import fnmatch
import logging
from pathlib import Path
from typing import Callable

from echo_mod_checker.domain.models import CheckSummary, ExpectedMod, JarMetadata, ModResult
from echo_mod_checker.domain.statuses import ModStatus, Severity
from echo_mod_checker.domain.versioning import VersionRelation, compare_versions
from echo_mod_checker.infrastructure.jar_scanner import scan_jars
from echo_mod_checker.infrastructure.manifest_reader import read_manifest

StageCallback = Callable[[str], None]
ProgressCallback = Callable[[int, int, Path], None]
LOGGER = logging.getLogger(__name__)


def run_check(
    mods_path: Path,
    manifest_path: Path,
    on_stage: StageCallback | None = None,
    on_progress: ProgressCallback | None = None,
) -> CheckSummary:
    if on_stage:
        on_stage("Читаем manifest.json и gradle.properties")
    expected = read_manifest(manifest_path)
    if on_stage:
        on_stage("Проверяем JAR")
    jars = scan_jars(mods_path, on_progress)
    LOGGER.info("Paths: mods=%s manifest=%s", mods_path, manifest_path)
    LOGGER.info("JAR count: %s", len(jars))
    if on_stage:
        on_stage("Собираем результат")
    results = _build_results(expected, jars)
    LOGGER.info("Results: ok=%s warnings=%s critical=%s", _count_ok(results), _count_warnings(results), _count_critical(results))
    return CheckSummary(mods_path, manifest_path, len(jars), tuple(results))


def _build_results(expected: list[ExpectedMod], jars: list[JarMetadata]) -> list[ModResult]:
    results: list[ModResult] = []
    by_id: dict[str, list[JarMetadata]] = defaultdict(list)
    linked_errors: dict[str, list[JarMetadata]] = defaultdict(list)
    expected_by_id = {mod.mod_id: mod for mod in expected}

    for jar in jars:
        if jar.metadata_error:
            owner = _find_error_owner(jar, expected)
            if owner:
                linked_errors[owner.mod_id].append(jar)
            else:
                LOGGER.debug("Ignored non-Echo JAR issue: %s (%s)", jar.jar_path, jar.metadata_error)
        elif jar.mod_id:
            by_id[jar.mod_id].append(jar)

    for mod in expected:
        installed = by_id.get(mod.mod_id, [])
        errored = linked_errors.get(mod.mod_id, [])
        if installed:
            results.append(_installed_result(mod, installed))
        elif errored:
            results.append(_metadata_result(errored[0], mod))
        elif mod.required:
            results.append(_missing_result(mod, ModStatus.MISSING_REQUIRED))
        else:
            results.append(_missing_result(mod, ModStatus.MISSING_OPTIONAL))

    for mod_id, installed in by_id.items():
        if mod_id not in expected_by_id and _is_echo_mod(installed[0]):
            results.append(_unknown_result(installed))
    return sorted(results, key=_result_sort_key)


def _installed_result(mod: ExpectedMod, installed: list[JarMetadata]) -> ModResult:
    primary = installed[0]
    if len(installed) > 1:
        return ModResult(
            status=ModStatus.DUPLICATE,
            name=mod.name,
            mod_id=mod.mod_id,
            required_version=mod.version,
            installed_version=primary.version,
            loader=primary.loader,
            jar_path=primary.jar_path,
            reason="Найдено несколько JAR с одинаковым mod id.",
            required=mod.required,
            duplicates=tuple(jar.jar_path for jar in installed),
        )
    relation = compare_versions(primary.version or "", mod.version)
    if relation is VersionRelation.EQUAL:
        return _version_result(ModStatus.OK, mod, primary, "Версия совпадает с ожидаемой.")
    if relation is VersionRelation.OUTDATED:
        return _version_result(ModStatus.OUTDATED, mod, primary, f"Требуется {mod.version}.")
    if relation is VersionRelation.NEWER:
        return _version_result(ModStatus.NEWER, mod, primary, f"В ожидаемом списке указана {mod.version}.")
    return _version_result(ModStatus.INVALID_VERSION, mod, primary, "Версию нельзя сравнить корректно.")


def _version_result(status: ModStatus, mod: ExpectedMod, jar: JarMetadata, reason: str) -> ModResult:
    return ModResult(
        status=status,
        name=mod.name,
        mod_id=mod.mod_id,
        required_version=mod.version,
        installed_version=jar.version,
        loader=jar.loader,
        jar_path=jar.jar_path,
        reason=reason,
        required=mod.required,
    )


def _missing_result(mod: ExpectedMod, status: ModStatus) -> ModResult:
    return ModResult(
        status=status,
        name=mod.name,
        mod_id=mod.mod_id,
        required_version=mod.version,
        reason="Мод не найден в папке mods.",
        required=mod.required,
    )


def _metadata_result(jar: JarMetadata, mod: ExpectedMod | None = None) -> ModResult:
    status = ModStatus.BAD_JAR if jar.corrupted else ModStatus.UNRECOGNIZED if jar.unrecognized else ModStatus.BAD_METADATA
    return ModResult(
        status=status,
        name=(mod.name if mod else jar.name) or jar.jar_path.stem,
        mod_id=mod.mod_id if mod else jar.mod_id,
        required_version=mod.version if mod else None,
        installed_version=jar.version,
        loader=jar.loader,
        jar_path=jar.jar_path,
        reason=jar.metadata_error or "Метаданные JAR не удалось прочитать.",
        required=mod.required if mod else False,
    )


def _unknown_result(installed: list[JarMetadata]) -> ModResult:
    primary = installed[0]
    return ModResult(
        status=ModStatus.UNKNOWN_ECHO if len(installed) == 1 else ModStatus.DUPLICATE,
        name=primary.name or primary.mod_id or primary.jar_path.stem,
        mod_id=primary.mod_id,
        installed_version=primary.version,
        loader=primary.loader,
        jar_path=primary.jar_path,
        reason="Echo-мод найден в папке mods, но отсутствует в manifest.json.",
        duplicates=tuple(jar.jar_path for jar in installed),
    )


def _is_echo_mod(jar: JarMetadata) -> bool:
    mod_id = (jar.mod_id or "").casefold()
    return mod_id.startswith("echo_") or mod_id.startswith("echo-")


def _find_error_owner(jar: JarMetadata, expected: list[ExpectedMod]) -> ExpectedMod | None:
    for mod in expected:
        if jar.mod_id == mod.mod_id:
            return mod
    for mod in expected:
        if _matches_file_pattern(jar.jar_path.name, mod.file_patterns):
            return mod
    return None


def _matches_file_pattern(file_name: str, patterns: tuple[str, ...]) -> bool:
    folded = file_name.casefold()
    return any(fnmatch.fnmatchcase(folded, pattern.casefold()) for pattern in patterns)


def _result_sort_key(result: ModResult) -> tuple[int, str]:
    return (0 if result.status.is_problem else 1, -result.severity.value, result.name.casefold())


def _count_ok(results: list[ModResult]) -> int:
    return sum(1 for result in results if result.severity is Severity.OK)


def _count_warnings(results: list[ModResult]) -> int:
    return sum(1 for result in results if result.severity is Severity.WARNING)


def _count_critical(results: list[ModResult]) -> int:
    return sum(1 for result in results if result.severity is Severity.CRITICAL)
