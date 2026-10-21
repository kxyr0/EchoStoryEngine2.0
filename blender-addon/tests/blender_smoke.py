from __future__ import annotations

import json
import importlib.util
import os
import sys
import tempfile
from pathlib import Path

import bpy


WORKSPACE_PARENT = Path(__file__).resolve().parents[2]
ADDON_PARENT = Path(os.environ.get("ECHO_ADDON_PARENT", WORKSPACE_PARENT))
sys.path.insert(0, str(ADDON_PARENT))

import echo_animation_exporter as addon
from echo_animation_exporter.animation.exporter import PoseValue, render_clip
from echo_animation_exporter.skin.exporter import build_skin_model
from echo_animation_exporter.skin.writer import write_skin_json


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def require_numbers_close(left, right, label: str) -> None:
    if isinstance(left, dict) and isinstance(right, dict):
        require(set(left) == set(right), f"{label}: object fields differ")
        for key in left:
            require_numbers_close(left[key], right[key], f"{label}.{key}")
        return
    if isinstance(left, list) and isinstance(right, list):
        require(len(left) == len(right), f"{label}: list lengths differ")
        for index, (left_value, right_value) in enumerate(zip(left, right)):
            require_numbers_close(left_value, right_value, f"{label}[{index}]")
        return
    if isinstance(left, (int, float)) and isinstance(right, (int, float)):
        require(abs(float(left) - float(right)) < 1.0e-6, f"{label}: {left} != {right}")
        return
    require(left == right, f"{label}: {left!r} != {right!r}")


def create_fixture():
    armature_data = bpy.data.armatures.new("SmokeRig")
    armature = bpy.data.objects.new("SmokeRig", armature_data)
    bpy.context.collection.objects.link(armature)
    bpy.context.view_layer.objects.active = armature
    armature.select_set(True)
    bpy.ops.object.mode_set(mode="EDIT")

    upper = armature_data.edit_bones.new("upper_arm")
    upper.head = (0.0, 0.0, 0.0)
    upper.tail = (0.0, 0.0, 1.0)
    upper.use_deform = True
    forearm = armature_data.edit_bones.new("forearm")
    forearm.head = upper.tail
    forearm.tail = (0.0, 0.0, 2.0)
    forearm.parent = upper
    forearm.use_connect = True
    forearm.use_deform = True
    bpy.ops.object.mode_set(mode="OBJECT")

    armature_data.echo_rig_id = "test:smooth_elbow"
    armature_data.echo_rig_schema = "AUTO"

    mesh_data = bpy.data.meshes.new("SmokeMesh")
    mesh_data.from_pydata(
        [(-0.5, 0.0, 0.0), (0.5, 0.0, 0.0), (0.5, 0.0, 2.0), (-0.5, 0.0, 2.0)],
        [],
        [(0, 1, 2, 3)],
    )
    mesh_data.update()
    uv_layer = mesh_data.uv_layers.new(name="UVMap")
    uv_by_vertex = ((0.0, 0.0), (1.0, 0.0), (1.0, 1.0), (0.0, 1.0))
    for loop in mesh_data.loops:
        uv_layer.data[loop.index].uv = uv_by_vertex[loop.vertex_index]

    mesh = bpy.data.objects.new("Body", mesh_data)
    bpy.context.collection.objects.link(mesh)
    upper_group = mesh.vertex_groups.new(name="upper_arm")
    upper_group.add([0, 1], 1.0, "REPLACE")
    upper_group.add([2, 3], 0.25, "REPLACE")
    forearm_group = mesh.vertex_groups.new(name="forearm")
    forearm_group.add([2, 3], 0.75, "REPLACE")
    modifier = mesh.modifiers.new(name="Armature", type="ARMATURE")
    modifier.object = armature
    return armature, mesh


def main() -> None:
    addon.register()
    try:
        armature, mesh = create_fixture()
        result = build_skin_model((mesh,))
        model = result.model
        require(set(model) == {"formatVersion", "bones", "meshes"}, "root contract drift")
        require(model["formatVersion"] == 2, "wrong formatVersion")
        require(result.schema.rig_resource_id == "test:smooth_elbow", "rig id not resolved")
        require([bone["name"] for bone in model["bones"]] == ["upper_arm", "forearm"], "bone order drift")
        require([bone["parent"] for bone in model["bones"]] == [-1, 0], "parent indices drift")
        require(all(len(bone["bindLocal"]) == 16 for bone in model["bones"]), "bind matrix size")
        require(all(len(bone["inverseBind"]) == 16 for bone in model["bones"]), "inverse bind size")
        require(len(model["meshes"]) == 1, "mesh count")
        exported_mesh = model["meshes"][0]
        require(set(exported_mesh) == {"name", "vertices", "triangles"}, "mesh contract drift")
        require(len(exported_mesh["triangles"]) == 6, "quad was not triangulated")
        require(len(exported_mesh["vertices"]) == 6, "loop vertices were not split")
        for vertex in exported_mesh["vertices"]:
            require(set(vertex) == {"position", "normal", "uv", "boneIndices", "boneWeights"}, "vertex contract drift")
            require(1 <= len(vertex["boneIndices"]) <= 4, "influence limit")
            require(abs(sum(vertex["boneWeights"]) - 1.0) < 1.0e-6, "weights not normalized")

        with tempfile.TemporaryDirectory(prefix="echo-skin-smoke-") as directory:
            path = write_skin_json(Path(directory) / "smooth_elbow", model)
            require(path.name == "smooth_elbow.skin.json", "file extension")
            loaded = json.loads(path.read_text(encoding="utf-8"))
            require(loaded == model, "JSON round-trip")

            reference_path = Path(
                r"D:\JDHSeason\mods\echo-animation\tools\blender\echo_skinned_export.py"
            )
            if reference_path.exists():
                spec = importlib.util.spec_from_file_location("echo_reference_exporter", reference_path)
                reference = importlib.util.module_from_spec(spec)
                spec.loader.exec_module(reference)
                bpy.ops.object.select_all(action="DESELECT")
                mesh.select_set(True)
                bpy.context.view_layer.objects.active = mesh
                reference_file = Path(directory) / "reference.skin.json"
                reference.export(str(reference_file), mesh.name)
                reference_model = json.loads(reference_file.read_text(encoding="utf-8"))
                require_numbers_close(model, reference_model, "Java reference exporter")

        tracks = {
            "root": [
                (
                    "0.0",
                    PoseValue((0.0, 0.0, 0.0), (0.0, 0.0, 0.0), (1.0, 1.0, 1.0)),
                    None,
                )
            ]
        }
        legacy = render_clip("SmokeClip", "jdh:smoke", tracks, "HOLD")
        custom = render_clip("SmokeClip", "jdh:smoke", tracks, "HOLD", "test:smooth_elbow")
        require('rig: "jdh:humanoid"' in legacy, "legacy rig id changed")
        require('rig: "test:smooth_elbow"' in custom, "armature rig id cannot reach writer")
        print(
            "ECHO_SKIN_SMOKE_OK",
            len(model["bones"]),
            result.vertex_count,
            result.triangle_count,
            len(result.warnings),
        )
    finally:
        addon.unregister()


main()
