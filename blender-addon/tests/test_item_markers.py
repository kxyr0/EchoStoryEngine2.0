import pathlib
import sys
import types
from dataclasses import replace

animation_path = pathlib.Path(__file__).resolve().parents[1] / "animation"
package = types.ModuleType("animation")
package.__path__ = [str(animation_path)]
sys.modules["animation"] = package

from animation.model import ExportAnimationModel, ExportEvent, ExportSegment
from animation.serialization import render_animation


def test_exact_item_markers_are_exported_to_high_level_animation():
    model = ExportAnimationModel(
        resource_id="jdh:take_book",
        rig_id="jdh:humanoid",
        duration_ticks=20,
        animation_type="ONE_SHOT",
        channel="BODY",
        bone_mask="BODY",
        source_state="idle",
        active_state="take",
        target_state="idle",
        return_state="idle",
        segments=(ExportSegment("take", 0, 20, "main"),),
        events=(ExportEvent("echo_item_attachments:attach_right_hand", 8),
                ExportEvent("echo_item_attachments:release", 14)),
    )
    text = render_animation(model)
    assert 'Marker("echo_item_attachments:attach_right_hand", phase: MAIN, tick: 8);' in text
    assert 'Marker("echo_item_attachments:release", phase: MAIN, tick: 14);' in text


def test_shifting_only_release_frame_changes_only_exported_marker_tick():
    base = ExportAnimationModel(
        resource_id="jdh:throw_book", rig_id="jdh:humanoid", duration_ticks=30,
        animation_type="ONE_SHOT", channel="BODY", bone_mask="BODY",
        source_state="idle", active_state="throw", target_state="idle", return_state="idle",
        segments=(ExportSegment("throw", 0, 30, "main"),),
        events=(ExportEvent("echo_item_attachments:release", 21),),
    )
    shifted = replace(base, events=(ExportEvent("echo_item_attachments:release", 25),))
    before = render_animation(base)
    after = render_animation(shifted)
    assert 'Marker("echo_item_attachments:release", phase: MAIN, tick: 21);' in before
    assert 'Marker("echo_item_attachments:release", phase: MAIN, tick: 25);' in after
    assert before.replace("tick: 21", "tick: 25") == after


if __name__ == "__main__":
    test_exact_item_markers_are_exported_to_high_level_animation()
    test_shifting_only_release_frame_changes_only_exported_marker_tick()
    print("item marker export: ok")
