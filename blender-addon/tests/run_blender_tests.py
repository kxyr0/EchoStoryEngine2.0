from __future__ import annotations

import sys
import tempfile
import unittest
from pathlib import Path

import bpy


ADDONS = Path(__file__).resolve().parents[2]
if str(ADDONS) not in sys.path:
    sys.path.insert(0, str(ADDONS))

import echo_animation_exporter as addon

addon.register()

from echo_animation_exporter.animation.fileio import atomic_write, overwrite_allowed
from echo_animation_exporter.animation.markers import STATE_END, STATE_START, add_semantic, marker_frame, set_marker
from echo_animation_exporter.animation.metadata import initialize
from echo_animation_exporter.animation.migration import migration_layout
from echo_animation_exporter.animation.model import ExportAnimationModel, ExportEvent, ExportKey, ExportPoint, ExportSegment, ExportTrack, RootMotion
from echo_animation_exporter.animation.naming import resource_id, semantic_names, valid_resource_id
from echo_animation_exporter.animation.preview import capture_scene_state, restore_scene_state
from echo_animation_exporter.animation.serialization import HEADER, render_animation, render_clip
from echo_animation_exporter.animation.timeline import build_points_events, build_segments, duration_ticks, frame_to_tick


def make_action(name="sample", kind="ONE_SHOT", start=0, end=20):
    action = bpy.data.actions.new(name)
    initialize(action, action.echo_animation, kind, f"jdh:{name}", frame_start=start, frame_end=end)
    return action, action.echo_animation


def make_model(kind="ONE_SHOT", segments=(), points=(), events=(), hold=False, root="DISABLED"):
    key = ExportKey(0, (0.0, 0.0, 0.0), (0.0, 0.0, 0.0), (1.0, 1.0, 1.0))
    return ExportAnimationModel("jdh:test", "jdh:humanoid", 20, kind, "BODY", "BODY", "idle", "active", "idle", "idle", tuple(segments), tuple(points), tuple(events), (ExportTrack("body", (key,)),), RootMotion(root, "root"), hold_last_frame=hold)


class EchoCoreTests(unittest.TestCase):
    def tearDown(self):
        for action in list(bpy.data.actions):
            if action.name.startswith("test_") or action.name.startswith("sample"):
                bpy.data.actions.remove(action)

    def test_01_action_metadata_initialization(self):
        action, meta = make_action("test_init")
        self.assertTrue(meta.enabled)
        self.assertEqual("jdh:test_init", meta.resource_id)

    def test_02_one_shot_needs_no_markers(self):
        action, meta = make_action("test_once")
        self.assertEqual((), build_segments(action, meta, 20))

    def test_03_full_action_loop(self):
        action, meta = make_action("test_loop", "LOOP")
        segment = build_segments(action, meta, 20)[0]
        self.assertEqual((0, 20, "loop"), (segment.start_tick, segment.end_tick, segment.role))

    def test_04_stateful_loop_with_enter_exit(self):
        action, meta = make_action("test_stateful", "STATEFUL_LOOP")
        meta.enter_enabled = meta.exit_enabled = meta.loop_enabled = True
        set_marker(action, STATE_START, 4, "Loop Start")
        set_marker(action, STATE_END, 16, "Loop End")
        self.assertEqual(["enter", "loop", "exit"], [item.role for item in build_segments(action, meta, 20)])

    def test_05_stateful_without_enter(self):
        action, meta = make_action("test_no_enter", "STATEFUL_LOOP")
        meta.enter_enabled, meta.exit_enabled, meta.loop_enabled = False, True, True
        set_marker(action, STATE_START, 0, "Loop Start")
        set_marker(action, STATE_END, 16, "Loop End")
        self.assertEqual(["loop", "exit"], [item.role for item in build_segments(action, meta, 20)])

    def test_06_stateful_without_exit(self):
        action, meta = make_action("test_no_exit", "STATEFUL_LOOP")
        meta.enter_enabled, meta.exit_enabled, meta.loop_enabled = True, False, True
        set_marker(action, STATE_START, 4, "Loop Start")
        set_marker(action, STATE_END, 20, "Loop End")
        self.assertEqual(["enter", "loop"], [item.role for item in build_segments(action, meta, 20)])

    def test_07_stateful_hold(self):
        action, meta = make_action("test_hold", "STATEFUL_HOLD")
        meta.enter_enabled = meta.exit_enabled = True
        points, _events = build_points_events(action, meta, 20, build_segments(action, meta, 20))
        self.assertEqual("HOLD", points[0].kind)

    def test_08_one_shot_hold_last_frame(self):
        text = render_animation(make_model("ONE_SHOT_HOLD", hold=True))
        self.assertIn("HoldLastFrame();", text)

    def test_09_locomotion_loop_only(self):
        action, meta = make_action("test_run", "LOCOMOTION")
        self.assertEqual(["loop"], [item.role for item in build_segments(action, meta, 20)])

    def test_10_locomotion_start_stop(self):
        action, meta = make_action("test_run_state", "LOCOMOTION", 0, 17)
        meta.enter_enabled = meta.exit_enabled = True
        set_marker(action, STATE_START, 2, "Loop Start")
        set_marker(action, STATE_END, 14, "Loop End")
        self.assertEqual([(0, 2), (2, 14), (14, 17)], [(item.start_tick, item.end_tick) for item in build_segments(action, meta, 17)])

    def test_11_foot_contacts_are_typed_points(self):
        action, meta = make_action("test_feet", "LOCOMOTION")
        add_semantic(meta, action, "LEFT_FOOT", "left_contact", 5)
        add_semantic(meta, action, "LEFT_FOOT", "left_contact", 8)
        add_semantic(meta, action, "RIGHT_FOOT", "right_contact", 15)
        points, _events = build_points_events(action, meta, 20, build_segments(action, meta, 20))
        self.assertEqual({"LEFT_FOOT", "RIGHT_FOOT"}, {item.kind for item in points})
        self.assertIn("left_contact_2", {item.name for item in points})

    def test_12_events_and_points(self):
        action, meta = make_action("test_events", "LOOP")
        add_semantic(meta, action, "EVENT", "step", 5, "stone")
        add_semantic(meta, action, "POINT", "neutral", 10)
        points, events = build_points_events(action, meta, 20, build_segments(action, meta, 20))
        self.assertEqual(("neutral", "step", "stone"), (points[0].name, events[0].name, events[0].payload))

    def test_13_semantic_segment_names(self):
        names = semantic_names("idle", "sitting", "idle")
        self.assertEqual(("idle_to_sitting", "sitting_loop", "sitting_to_idle"), (names["enter"], names["loop"], names["exit"]))

    def test_14_frames_to_ticks(self):
        self.assertEqual(17, duration_ticks(0, 40, 48.0, 20.0))

    def test_15_boundary_ticks_are_shared(self):
        self.assertEqual(8, frame_to_tick(8, 0, 40, 40))

    def test_16_resource_validation(self):
        self.assertTrue(valid_resource_id("jdh:humanoid_run"))
        self.assertFalse(valid_resource_id("Humanoid Run"))

    def test_17_resource_normalization(self):
        self.assertEqual("jdh:humanoid_run", resource_id("Humanoid Run"))

    def test_18_clip_is_one_resource(self):
        segment = ExportSegment("run_loop", 0, 20, "loop")
        text = render_clip(make_model("LOCOMOTION", (segment,)))
        self.assertEqual(1, text.count('Id("jdh:test")'))
        self.assertNotIn("_enter.animation-clip", text)

    def test_19_loop_only_animation_has_no_enter_exit(self):
        segment = ExportSegment("idle_loop", 0, 20, "loop")
        text = render_animation(make_model("LOOP", (segment,)))
        self.assertNotIn("Enter(", text)
        self.assertNotIn("Exit(", text)

    def test_20_event_serialization(self):
        text = render_clip(make_model(events=(ExportEvent("step", 5, "stone"),)))
        self.assertIn('Event("step", at: 5, payload: "stone");', text)

    def test_21_point_serialization(self):
        segment = ExportSegment("run_loop", 0, 20, "loop")
        text = render_clip(make_model("LOOP", (segment,), (ExportPoint("neutral", 10, "run_loop"),)))
        self.assertIn('Point(runLoop, "neutral", at: 0.5);', text)

    def test_22_root_motion_serialization(self):
        self.assertIn('RootMotion(Extract, bone: "root");', render_clip(make_model(root="EXTRACT")))

    def test_23_manual_file_protection(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "manual.animation.es"
            path.write_text("manual", encoding="utf-8")
            self.assertFalse(overwrite_allowed(path, "GENERATED")[0])

    def test_24_generated_file_overwrite(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "generated.animation.es"
            path.write_text(HEADER + "\nold", encoding="utf-8")
            self.assertTrue(overwrite_allowed(path, "GENERATED")[0])

    def test_25_atomic_write(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "a" / "clip.es"
            atomic_write(path, "complete")
            self.assertEqual("complete", path.read_text(encoding="utf-8"))

    def test_26_migration_layout(self):
        enter, _ = make_action("test_enter", start=0, end=2)
        loop, _ = make_action("test_loop_source", start=0, end=12)
        exit_action, _ = make_action("test_exit", start=0, end=3)
        enter["manual_span"], loop["manual_span"], exit_action["manual_span"] = 2, 12, 3
        # Empty test Actions use the fallback range; layout arithmetic is covered by the humanoid integration test.
        self.assertIn("state_start", migration_layout(enter, loop, exit_action))

    def test_27_action_markers_do_not_mix(self):
        first, _ = make_action("test_first", "LOOP")
        second, _ = make_action("test_second", "LOOP")
        set_marker(first, STATE_START, 3, "Loop Start")
        set_marker(second, STATE_START, 7, "Loop Start")
        self.assertEqual((3, 7), (marker_frame(first, STATE_START), marker_frame(second, STATE_START)))

    def test_28_preview_restores_scene(self):
        scene = bpy.context.scene
        scene.frame_start, scene.frame_end = 1, 10
        scene.frame_set(4)
        snapshot = capture_scene_state(scene)
        scene.frame_start, scene.frame_end = 20, 40
        scene.frame_set(30)
        restore_scene_state(scene, snapshot)
        self.assertEqual((snapshot[0], snapshot[1], snapshot[2]), (scene.frame_current, scene.frame_start, scene.frame_end))

    def test_29_mutating_operators_are_undoable(self):
        from echo_animation_exporter.animation.operators import ECHO_OT_create_boundary_keys, ECHO_OT_set_boundary
        self.assertIn("UNDO", ECHO_OT_create_boundary_keys.bl_options)
        self.assertIn("UNDO", ECHO_OT_set_boundary.bl_options)


if __name__ == "__main__":
    suite = unittest.defaultTestLoader.loadTestsFromTestCase(EchoCoreTests)
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    addon.unregister()
    raise SystemExit(0 if result.wasSuccessful() else 1)
