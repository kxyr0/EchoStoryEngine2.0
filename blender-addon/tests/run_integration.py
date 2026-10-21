from __future__ import annotations

import math
import sys
from pathlib import Path

import bpy


ADDONS = Path(__file__).resolve().parents[2]
if str(ADDONS) not in sys.path:
    sys.path.insert(0, str(ADDONS))

import echo_animation_exporter as addon

addon.register()

from echo_animation_exporter.animation.export_service import export_action
from echo_animation_exporter.animation.markers import STATE_END, STATE_START, add_semantic, set_marker
from echo_animation_exporter.animation.metadata import initialize, normalize_segment_names
from echo_animation_exporter.animation.migration import merge_actions
from echo_animation_exporter.rig.schema import LEGACY_BONE_ORDER, LEGACY_PARENT


OUTPUT = Path(__file__).resolve().parent / "generated"


def create_rig():
    data = bpy.data.armatures.new("EchoHumanoid")
    armature = bpy.data.objects.new("EchoHumanoid", data)
    bpy.context.collection.objects.link(armature)
    bpy.context.view_layer.objects.active = armature
    armature.select_set(True)
    bpy.ops.object.mode_set(mode="EDIT")
    bones = {}
    for index, name in enumerate(LEGACY_BONE_ORDER):
        bone = data.edit_bones.new(name)
        bone.head = (0.0, 0.0, float(index) * 0.25)
        bone.tail = (0.0, 0.0, float(index) * 0.25 + 0.2)
        bones[name] = bone
    for name, parent in LEGACY_PARENT.items():
        if parent:
            bones[name].parent = bones[parent]
    bpy.ops.object.mode_set(mode="POSE")
    return armature


def keyed_action(armature, name, kind, end, keys):
    action = bpy.data.actions.new(name)
    action.use_fake_user = True
    armature.animation_data_create().action = action
    bone = armature.pose.bones["body"]
    bone.rotation_mode = "XYZ"
    for frame, degrees in keys:
        bpy.context.scene.frame_set(frame)
        bone.rotation_euler = (0.0, 0.0, math.radians(degrees))
        bone.keyframe_insert(data_path="rotation_euler", frame=frame, group="body")
    initialize(action, action.echo_animation, kind, f"jdh:{name}", armature.data.echo_rig_id, 0, end)
    meta = action.echo_animation
    meta.output_directory = str(OUTPUT)
    meta.resource_folder = "tests"
    meta.overwrite_policy = "GENERATED"
    return action


def main():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    bpy.context.scene.render.fps = 20
    armature = create_rig()
    cases = []
    idle = keyed_action(armature, "test_idle", "LOOP", 8, ((0, 0), (4, 4), (8, 0)))
    cases.append(idle)
    one_shot = keyed_action(armature, "test_one_shot", "ONE_SHOT", 6, ((0, 0), (6, 25)))
    cases.append(one_shot)
    stateful = keyed_action(armature, "test_stateful", "STATEFUL_LOOP", 8, ((0, 0), (2, 10), (4, 16), (6, 10), (8, 0)))
    stateful.echo_animation.enter_enabled = stateful.echo_animation.exit_enabled = True
    set_marker(stateful, STATE_START, 2, "Loop Start")
    set_marker(stateful, STATE_END, 6, "Loop End")
    armature.animation_data.action = stateful
    assert bpy.ops.echo.create_boundary_keys() == {"FINISHED"}
    cases.append(stateful)
    legacy_enter = keyed_action(armature, "humanoid_run_enter", "ONE_SHOT", 2, ((0, 0), (2, 8)))
    legacy_loop = keyed_action(armature, "humanoid_run_loop", "LOOP", 12, ((0, 8), (4, 18), (8, -4), (12, 8)))
    legacy_exit = keyed_action(armature, "humanoid_run_exit", "ONE_SHOT", 3, ((0, 8), (3, 0)))
    settings = bpy.context.window_manager.echo_animation_migration
    settings.enter_action, settings.loop_action, settings.exit_action = legacy_enter, legacy_loop, legacy_exit
    settings.new_name = "humanoid_run"
    settings.source_state, settings.active_state, settings.target_state = "idle", "run", "idle"
    run = merge_actions(bpy.context, armature, settings)
    run.echo_animation.output_directory = str(OUTPUT)
    run.echo_animation.resource_folder = "tests"
    run.echo_animation.overwrite_policy = "GENERATED"
    run.echo_animation.authored_speed = 4.3
    run.echo_animation.active_state = "run"
    normalize_segment_names(run.echo_animation, force=True)
    set_marker(run, STATE_START, 2, "Loop Start")
    set_marker(run, STATE_END, 14, "Loop End")
    add_semantic(run.echo_animation, run, "LEFT_FOOT", "left_contact", 5, segment=run.echo_animation.loop_segment_name)
    add_semantic(run.echo_animation, run, "RIGHT_FOOT", "right_contact", 11, segment=run.echo_animation.loop_segment_name)
    add_semantic(run.echo_animation, run, "EVENT", "footstep", 5, "left")
    cases.append(run)
    results = []
    for action in cases:
        armature.animation_data.action = action
        result = export_action(bpy.context, armature, action)
        results.append(result)
        print("ECHO_INTEGRATION_EXPORT", action.name, result.clip_path, result.animation_path)
    expected = {"test_idle", "test_one_shot", "test_stateful", "humanoid_run"}
    clips = {path.stem.replace(".animation-clip", "") for path in (OUTPUT / "animation_clips" / "tests").glob("*.animation-clip.es")}
    assert expected <= clips, (expected, clips)
    assert not list(OUTPUT.rglob("humanoid_run_enter.animation-clip.es"))
    assert not list(OUTPUT.rglob("humanoid_run_loop.animation-clip.es"))
    assert not list(OUTPUT.rglob("humanoid_run_exit.animation-clip.es"))
    print("ECHO_INTEGRATION_OK", len(results))


try:
    main()
finally:
    addon.unregister()
