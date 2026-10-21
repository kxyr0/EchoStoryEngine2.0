"""Verify EA1 production jars, shared library identity, and JUnit reports."""
from pathlib import Path
import hashlib
import json
import sys
import tomllib
import xml.etree.ElementTree as ET
import zipfile

mods = Path(__file__).resolve().parents[2]
releases = [
    ('echo_dialogue', '0.15.0', mods / 'echo-dialogue/dialogue-forge-1.20.1/build/libs/echo-dialogue-0.15.0-all.jar'),
    ('echo_voice', '0.2.1', mods / 'echo-voice/voice-forge-1.20.1/build/libs/echo-voice-0.2.1-all.jar'),
    ('echo_audio', '0.2.0', mods / 'echo-audio/audio-forge-1.20.1/build/libs/echo-audio-0.2.0-all.jar'),
    ('echo_scene', '0.2.0', mods / 'echo-scene/scene-forge-1.20.1/build/libs/echo-scene-0.2.0-all.jar'),
    ('echo_observation', '1.3.0', mods / 'echo-observation/build/libs/echo-observation-1.3.0-all.jar'),
    ('echo_story_engine', '1.11.0', mods / 'echo-story-engine/story-forge-1.20.1/build/libs/echo-story-engine-1.11.0-all.jar'),
]
library = mods / 'echo-audio/build/libs/echo-audio-0.1.0.jar'
expected_library = hashlib.sha256(library.read_bytes()).hexdigest()
report = {'shared_library_sha256': expected_library, 'jars': [], 'tests': []}
for mod_id, version, path in releases:
    with zipfile.ZipFile(path) as jar:
        metadata = tomllib.loads(jar.read('META-INF/mods.toml').decode('utf-8'))
        assert any(m['modId'] == mod_id and m['version'] == version for m in metadata['mods']), path
        forbidden = [n for n in jar.namelist() if n.startswith(('net/minecraft/', 'org/junit/'))]
        assert not forbidden, forbidden
        if mod_id != 'echo_voice':
            entries = json.loads(jar.read('META-INF/jarjar/metadata.json'))['jars']
            shared = [j for j in entries if j['identifier'] == {'group': 'io.github.kxyr0.echoaudio', 'artifact': 'echo-audio'}]
            assert len(shared) == 1, (path, entries)
            actual_library = hashlib.sha256(jar.read(shared[0]['path'])).hexdigest()
            assert actual_library == expected_library, (path, actual_library, expected_library)
    report['jars'].append({'mod_id': mod_id, 'version': version, 'path': str(path),
                           'sha256': hashlib.sha256(path.read_bytes()).hexdigest()})
for repo in ('echo-dialogue', 'echo-voice', 'echo-audio', 'echo-scene', 'echo-observation', 'echo-story-engine', 'echo-content-doctor'):
    counts = {'repo': repo, 'tests': 0, 'failures': 0, 'errors': 0, 'skipped': 0}
    project = mods / repo
    reports = list(project.glob('build/test-results/test/TEST-*.xml'))
    reports += list(project.glob('*/build/test-results/test/TEST-*.xml'))
    for path in reports:
        suite = ET.parse(path).getroot()
        for key in ('tests', 'failures', 'errors', 'skipped'):
            counts[key] += int(suite.get(key, 0))
    assert counts['tests'] and counts['failures'] == counts['errors'] == 0, counts
    report['tests'].append(counts)
if len(sys.argv) > 1:
    active_root = Path(sys.argv[1])
    active = {m[0]: [] for m in releases}
    for path in active_root.glob('*.jar'):
        with zipfile.ZipFile(path) as jar:
            if 'META-INF/mods.toml' not in jar.namelist():
                continue
            metadata = tomllib.loads(jar.read('META-INF/mods.toml').decode('utf-8-sig'))
            for entry in metadata.get('mods', []):
                if entry['modId'] in active:
                    active[entry['modId']].append(path)
    for release in report['jars']:
        paths = active[release['mod_id']]
        assert len(paths) == 1, (release['mod_id'], paths)
        assert hashlib.sha256(paths[0].read_bytes()).hexdigest() == release['sha256'], paths[0]
    report['active_duplicates'] = 0
output = mods / 'echo-audio/build/reports/ea1/release.json'
output.parent.mkdir(parents=True, exist_ok=True)
output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, ensure_ascii=False, indent=2))
