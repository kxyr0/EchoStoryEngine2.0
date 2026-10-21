from __future__ import annotations

import json
import zipfile
from pathlib import Path

from echo_mod_checker.application.install_service import install_missing_mods
from echo_mod_checker.domain.models import CheckSummary, ModResult
from echo_mod_checker.domain.statuses import ModStatus
from echo_mod_checker.infrastructure.manifest_reader import read_manifest


def write_manifest(path: Path, mods: list[dict[str, object]]) -> Path:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps({"echoMods": mods}, ensure_ascii=False), encoding="utf-8")
    return path


def write_gradle_properties(path: Path, mod_id: str, name: str, version: str, artifact: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        "\n".join(
            [
                f"mod_id={mod_id}",
                f"mod_name={name}",
                f"mod_version={version}",
                f"artifact_name={artifact}",
            ]
        ),
        encoding="utf-8",
    )


def write_forge_jar(path: Path, mod_id: str, name: str, version: str) -> Path:
    path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(path, "w") as archive:
        archive.writestr(
            "META-INF/mods.toml",
            "\n".join(["[[mods]]", f'modId="{mod_id}"', f'displayName="{name}"', f'version="{version}"']),
        )
    return path


def test_manifest_versions_are_refreshed_from_gradle_properties(tmp_path: Path) -> None:
    repo = tmp_path / "repo"
    manifest = write_manifest(
        repo / "echo_mod_checker" / "manifest.json",
        [{"id": "echo_test", "name": "Old Name", "version": "0.1.0", "required": False, "filePatterns": ["old-*.jar"]}],
    )
    write_gradle_properties(repo / "mods" / "echo-test" / "gradle.properties", "echo_test", "Echo Test", "2.0.0", "echo-test")

    mod = read_manifest(manifest)[0]

    assert mod.name == "Echo Test"
    assert mod.version == "2.0.0"
    assert mod.required is False
    assert mod.file_patterns == ("old-*.jar",)


def test_gradle_properties_add_new_echo_mods(tmp_path: Path) -> None:
    repo = tmp_path / "repo"
    manifest = write_manifest(repo / "echo_mod_checker" / "manifest.json", [])
    write_gradle_properties(repo / "mods" / "echo-gradle" / "gradle.properties", "echo_gradle", "Echo Gradle", "1.0.0", "echo-gradle")

    mods = read_manifest(manifest)

    assert [mod.mod_id for mod in mods] == ["echo_gradle"]
    assert mods[0].file_patterns == ("echo-gradle-*.jar",)


def test_install_missing_mod_copies_only_matching_metadata(tmp_path: Path) -> None:
    repo = tmp_path / "repo"
    mods_path = tmp_path / "instance" / "mods"
    mods_path.mkdir(parents=True)
    build_libs = repo / "mods" / "echo-missing" / "build" / "libs"
    write_forge_jar(build_libs / "echo-missing-harness-1.0.0.jar", "echo_missing_harness", "Harness", "1.0.0")
    source = write_forge_jar(build_libs / "echo-missing-1.0.0.jar", "echo_missing", "Echo Missing", "1.0.0")
    summary = CheckSummary(
        mods_path=mods_path,
        manifest_path=repo / "echo_mod_checker" / "manifest.json",
        jar_count=0,
        results=(ModResult(ModStatus.MISSING_REQUIRED, "Echo Missing", "echo_missing", "1.0.0", required=True),),
    )

    report = install_missing_mods(summary, repo)

    assert report.installed_count == 1
    assert report.items[0].source == source
    assert (mods_path / "echo-missing-1.0.0.jar").exists()
    assert not (mods_path / "echo-missing-harness-1.0.0.jar").exists()
