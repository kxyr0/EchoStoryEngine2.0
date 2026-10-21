from __future__ import annotations

from importlib.util import module_from_spec, spec_from_file_location
from pathlib import Path
import sys

PARSER_PATH = Path(__file__).resolve().parents[1] / "animation" / "es_parser.py"
spec = spec_from_file_location("echo_es_parser_test_module", PARSER_PATH)
assert spec and spec.loader
parser = module_from_spec(spec)
sys.modules[spec.name] = parser
spec.loader.exec_module(parser)
parse_animation_text = parser.parse_animation_text
parse_clip_text = parser.parse_clip_text


def run() -> None:
    vector_clip = '''
return AnimationClip("jdh:walk_main",
    rig: "jdh:humanoid",
    schemaVersion: 2,
    loopSeam: "SMOOTH",
    tracks: [
        Track("body", frames: [
            Frame("0.0", interpolation: "SMOOTH").Position(0,12,0).Rotation(0,0,0).Scale(1,1,1),
            Frame("1.0").Position(0,12,0).Rotation(0,5,0).Scale(1,1,1)
        ])
    ]);
'''
    clip = parse_clip_text(vector_clip)
    assert clip.resource_id == "jdh:walk_main"
    assert clip.loop_seam == "SMOOTH"
    assert len(clip.tracks) == 1
    assert len(clip.tracks[0].frames) == 2

    matrix_values = ", ".join(f'"{value}"' for value in range(16))
    matrix_clip = f'''
return AnimationClip("jdh:therig_main",
    rig: "jdh:therig",
    schemaVersion: 2,
    loopSeam: "HOLD",
    tracks: [Track("head", frames: [Frame("0.0").Matrix({matrix_values})])]);
'''
    clip = parse_clip_text(matrix_clip)
    assert clip.tracks[0].frames[0].matrix == tuple(float(value) for value in range(16))

    animation = parse_animation_text('''
return Animation("jdh:walk",
    channels: [Channel("body")],
    sources: [Source("body/idle")],
    entries: [Entry("body/idle", clip: "jdh:walk_enter", durationTicks: 3)],
    main: Main(clip: "jdh:walk_main", mode: "LOOP_UNTIL_STOP", durationTicks: 18,
        exitPoints: [ExitPoint("complete", progress: "1.0")]),
    exits: [Exit("complete", clip: "jdh:walk_exit", durationTicks: 4)],
    target: "body/idle");
''')
    assert animation.mode == "LOOP_UNTIL_STOP"
    assert [phase.duration_ticks for phase in animation.phases] == [3, 18, 4]


if __name__ == "__main__":
    run()
    print("Echo .es parser tests: OK")
