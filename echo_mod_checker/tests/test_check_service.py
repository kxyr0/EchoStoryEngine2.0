from __future__ import annotations

import json
import zipfile
from pathlib import Path

from echo_mod_checker.application.check_service import run_check
from echo_mod_checker.domain.statuses import ModStatus


def write_manifest(path: Path, mods: list[dict[str, object]]) -> Path:
    path.write_text(json.dumps({"echoMods": mods}, ensure_ascii=False), encoding="utf-8")
    return path


def write_jar(path: Path, metadata: dict[str, object] | str | None) -> Path:
    with zipfile.ZipFile(path, "w") as archive:
        if isinstance(metadata, dict):
            archive.writestr("fabric.mod.json", json.dumps(metadata, ensure_ascii=False))
        elif isinstance(metadata, str):
            archive.writestr("fabric.mod.json", metadata)
        else:
            archive.writestr("readme.txt", "no metadata")
    return path


def write_forge_jar(path: Path, mods: list[dict[str, str]], manifest_version: str | None = None) -> Path:
    lines = []
    for mod in mods:
        lines.extend(
            [
                "[[mods]]",
                f'modId="{mod["modId"]}"',
                f'displayName="{mod["displayName"]}"',
                f'version="{mod["version"]}"',
                "",
            ]
        )
    with zipfile.ZipFile(path, "w") as archive:
        archive.writestr("META-INF/mods.toml", "\n".join(lines))
        if manifest_version:
            archive.writestr("META-INF/MANIFEST.MF", f"Manifest-Version: 1.0\nImplementation-Version: {manifest_version}\n")
    return path


def expected(version: str = "1.2.0", required: bool = True) -> dict[str, object]:
    return {"id": "echo_test", "name": "Echo Test", "version": version, "required": required}


def expected_with_pattern(pattern: str, version: str = "1.2.0") -> dict[str, object]:
    mod = expected(version)
    mod["filePatterns"] = [pattern]
    return mod


def installed(version: str = "1.2.0", mod_id: str = "echo_test") -> dict[str, object]:
    return {"id": mod_id, "name": "Echo Test", "version": version}


def forge_installed(version: str = "1.2.0", mod_id: str = "echo_test") -> dict[str, str]:
    return {"modId": mod_id, "displayName": "Echo Test", "version": version}


def run_case(tmp_path: Path, mods: list[dict[str, object]]) -> tuple:
    manifest = write_manifest(tmp_path / "manifest.json", mods)
    summary = run_check(tmp_path, manifest)
    return summary.results


def test_current_version(tmp_path: Path) -> None:
    write_jar(tmp_path / "echo-test.jar", installed("1.2.0"))
    results = run_case(tmp_path, [expected("1.2.0")])
    assert results[0].status is ModStatus.OK


def test_forge_jar_with_mods_toml(tmp_path: Path) -> None:
    write_forge_jar(tmp_path / "echo-test.jar", [forge_installed("1.2.0")])
    results = run_case(tmp_path, [expected("1.2.0")])
    assert results[0].status is ModStatus.OK
    assert results[0].loader.value == "Forge"


def test_fabric_jar_with_fabric_mod_json(tmp_path: Path) -> None:
    write_jar(tmp_path / "echo-test.jar", installed("1.2.0"))
    results = run_case(tmp_path, [expected("1.2.0")])
    assert results[0].status is ModStatus.OK


def test_forge_jar_does_not_need_fabric_metadata(tmp_path: Path) -> None:
    write_forge_jar(tmp_path / "echo-test.jar", [forge_installed("1.2.0")])
    summary = run_check(tmp_path, write_manifest(tmp_path / "manifest.json", [expected("1.2.0")]))
    assert summary.issue_count == 0


def test_fabric_jar_does_not_need_mods_toml(tmp_path: Path) -> None:
    write_jar(tmp_path / "echo-test.jar", installed("1.2.0"))
    summary = run_check(tmp_path, write_manifest(tmp_path / "manifest.json", [expected("1.2.0")]))
    assert summary.issue_count == 0


def test_forge_jar_with_multiple_mod_ids(tmp_path: Path) -> None:
    write_forge_jar(
        tmp_path / "multi.jar",
        [forge_installed("1.2.0"), forge_installed("4.0.0", "third_party")],
    )
    results = run_case(tmp_path, [expected("1.2.0")])
    assert len(results) == 1
    assert results[0].status is ModStatus.OK


def test_forge_file_jar_version_reads_manifest(tmp_path: Path) -> None:
    write_forge_jar(
        tmp_path / "echo-test.jar",
        [forge_installed("${file.jarVersion}")],
        manifest_version="1.2.0",
    )
    results = run_case(tmp_path, [expected("1.2.0")])
    assert results[0].status is ModStatus.OK


def test_outdated_version(tmp_path: Path) -> None:
    write_jar(tmp_path / "echo-test.jar", installed("1.1.0"))
    results = run_case(tmp_path, [expected("1.2.0")])
    assert results[0].status is ModStatus.OUTDATED


def test_newer_version(tmp_path: Path) -> None:
    write_jar(tmp_path / "echo-test.jar", installed("1.3.0"))
    results = run_case(tmp_path, [expected("1.2.0")])
    assert results[0].status is ModStatus.NEWER


def test_missing_required_mod(tmp_path: Path) -> None:
    results = run_case(tmp_path, [expected(required=True)])
    assert results[0].status is ModStatus.MISSING_REQUIRED


def test_missing_optional_mod(tmp_path: Path) -> None:
    summary = run_check(tmp_path, write_manifest(tmp_path / "manifest.json", [expected(required=False)]))
    assert summary.results[0].status is ModStatus.MISSING_OPTIONAL
    assert summary.issue_count == 0


def test_duplicates(tmp_path: Path) -> None:
    write_jar(tmp_path / "a.jar", installed("1.2.0"))
    write_jar(tmp_path / "b.jar", installed("1.2.0"))
    results = run_case(tmp_path, [expected("1.2.0")])
    duplicate = next(result for result in results if result.status is ModStatus.DUPLICATE)
    assert len(duplicate.duplicates) == 2


def test_corrupted_jar(tmp_path: Path) -> None:
    (tmp_path / "broken.jar").write_bytes(b"not-a-zip")
    results = run_case(tmp_path, [])
    assert results == ()


def test_corrupted_expected_jar_uses_file_pattern(tmp_path: Path) -> None:
    (tmp_path / "echo-test-1.2.0.jar").write_bytes(b"not-a-zip")
    results = run_case(tmp_path, [expected_with_pattern("echo-test-*.jar")])
    assert results[0].status is ModStatus.BAD_JAR


def test_unrecognized_expected_jar_uses_file_pattern(tmp_path: Path) -> None:
    write_jar(tmp_path / "echo-test-1.2.0.jar", None)
    results = run_case(tmp_path, [expected_with_pattern("echo-test-*.jar")])
    assert results[0].status is ModStatus.UNRECOGNIZED


def test_missing_fabric_metadata(tmp_path: Path) -> None:
    write_jar(tmp_path / "no-meta.jar", None)
    results = run_case(tmp_path, [])
    assert results == ()


def test_invalid_metadata_json(tmp_path: Path) -> None:
    write_jar(tmp_path / "bad-json.jar", "{")
    results = run_case(tmp_path, [])
    assert results == ()


def test_invalid_expected_metadata_uses_file_pattern(tmp_path: Path) -> None:
    write_jar(tmp_path / "echo-test-1.2.0.jar", "{")
    results = run_case(tmp_path, [expected_with_pattern("echo-test-*.jar")])
    assert results[0].status is ModStatus.BAD_METADATA


def test_invalid_version(tmp_path: Path) -> None:
    write_jar(tmp_path / "bad-version.jar", installed("not a version"))
    results = run_case(tmp_path, [expected("1.2.0")])
    assert results[0].status is ModStatus.INVALID_VERSION


def test_third_party_forge_mod_is_not_reported(tmp_path: Path) -> None:
    write_forge_jar(tmp_path / "fastboot.jar", [forge_installed("1.0.0", "fastboot")])
    summary = run_check(tmp_path, write_manifest(tmp_path / "manifest.json", []))
    assert summary.results == ()
    assert summary.issue_count == 0


def test_unrecognized_third_party_jar_is_not_a_problem(tmp_path: Path) -> None:
    write_jar(tmp_path / "random-library.jar", None)
    summary = run_check(tmp_path, write_manifest(tmp_path / "manifest.json", []))
    assert summary.results == ()
    assert summary.issue_count == 0


def test_cyrillic_and_spaces_in_paths(tmp_path: Path) -> None:
    mods_dir = tmp_path / "Форма Мысли mods"
    mods_dir.mkdir()
    write_jar(mods_dir / "эхо тест.jar", installed("0.1.0-SNAPSHOT"))
    manifest = write_manifest(mods_dir / "manifest.json", [expected("0.1.0-SNAPSHOT")])
    summary = run_check(mods_dir, manifest)
    assert summary.results[0].status is ModStatus.OK


def test_summary_counts(tmp_path: Path) -> None:
    write_jar(tmp_path / "ok.jar", installed("1.2.0", "echo_ok"))
    write_jar(tmp_path / "old.jar", installed("1.0.0", "echo_old"))
    manifest = write_manifest(
        tmp_path / "manifest.json",
        [
            {"id": "echo_ok", "name": "Echo OK", "version": "1.2.0", "required": True},
            {"id": "echo_old", "name": "Echo Old", "version": "1.2.0", "required": True},
            {"id": "echo_missing", "name": "Echo Missing", "version": "1.0.0", "required": True},
        ],
    )
    summary = run_check(tmp_path, manifest)
    assert summary.checked_count == 1
    assert summary.warning_count == 1
    assert summary.critical_count == 1
    assert summary.issue_count == 2


def test_counter_contains_only_expected_echo_problems(tmp_path: Path) -> None:
    write_jar(tmp_path / "newer.jar", installed("2.0.0", "echo_newer"))
    write_forge_jar(tmp_path / "third-party.jar", [forge_installed("1.0.0", "fastboot")])
    write_jar(tmp_path / "unrecognized.jar", None)
    manifest = write_manifest(
        tmp_path / "manifest.json",
        [
            {"id": "echo_newer", "name": "Echo Newer", "version": "1.0.0", "required": True},
            {"id": "echo_missing_optional", "name": "Echo Optional", "version": "1.0.0", "required": False},
            {"id": "echo_missing_required", "name": "Echo Missing", "version": "1.0.0", "required": True},
        ],
    )
    summary = run_check(tmp_path, manifest)
    assert summary.count(ModStatus.NEWER) == 1
    assert summary.count(ModStatus.MISSING_OPTIONAL) == 1
    assert summary.count(ModStatus.MISSING_REQUIRED) == 1
    assert summary.issue_count == 1
