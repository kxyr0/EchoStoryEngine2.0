from __future__ import annotations

import json
import os
import sys
import tempfile
from pathlib import Path

import bpy
from mathutils import Vector


WORKSPACE_PARENT = Path(__file__).resolve().parents[2]
ADDON_PARENT = Path(os.environ.get("ECHO_ADDON_PARENT", WORKSPACE_PARENT))
sys.path.insert(0, str(ADDON_PARENT))

import echo_animation_exporter as addon
from echo_animation_exporter.animation.exporter import (
    render_clip,
    sample_schema_pose_value,
    schema_base_pose_values,
    validate_rig,
)
from echo_animation_exporter.rig.therig import THERIG_RIG_ID, THERIG_SCHEMA_ID
from echo_animation_exporter.rig.writer import render_rig_definition, write_rig_definition
from echo_animation_exporter.skin.exporter import (
    armature_for_mesh,
    build_skin_model,
    mesh_objects_from_context,
)
from echo_animation_exporter.skin.writer import write_skin_json


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def main() -> None:
    require(Path(bpy.data.filepath).name == "TheRig2.6.blend", "wrong Blender fixture")
    addon.register()
    try:
        armature = bpy.data.objects["TheRig"]
        for obj in bpy.context.selected_objects:
            obj.select_set(False)
        armature.select_set(True)
        bpy.context.view_layer.objects.active = armature

        body = bpy.data.objects["Body"]
        require(armature_for_mesh(body) == armature, "Body lattice did not resolve TheRig")
        meshes = mesh_objects_from_context(bpy.context)
        names = {obj.name for obj in meshes}
        for required in (
            "Body", "Head", "L_Arm4x4", "R_Arm4x4", "L_Leg", "R_Leg",
            "L_Eye", "R_Eye",
        ):
            require(required in names, f"visible TheRig mesh {required} was not discovered")
        require("L_Arm4x4_Fingers" not in names, "hidden mesh variant was exported")

        result = build_skin_model(meshes)
        require(result.schema.schema_id == THERIG_SCHEMA_ID, "TheRig profile was not auto-detected")
        require(result.schema.rig_resource_id == THERIG_RIG_ID, "TheRig default rig ID drift")
        require(48 < len(result.model["bones"]) <= 512, "TheRig full runtime skeleton was not exported")
        require(result.model["bones"][0]["name"] == "rig_root", "main TheRig root order drift")
        require(any(bone["name"] == "head" for bone in result.model["bones"]), "head missing")

        animation_schema = validate_rig(armature)
        base_values = schema_base_pose_values(armature, animation_schema, True)
        sampled = {}
        for bone_name in animation_schema.runtime_names:
            value, _euler = sample_schema_pose_value(
                armature, animation_schema, bone_name, None, True
            )
            sampled[bone_name] = value
        require(set(sampled) == set(base_values), "animation/profile bone contract drift")
        script = render_clip(
            "TheRigFixtureClip",
            "jdh:therig_fixture",
            {name: [("0.0", value, None)] for name, value in sampled.items()},
            "HOLD",
            animation_schema.rig_resource_id,
            bone_order=animation_schema.runtime_names,
        )
        require('rig: "jdh:therig_2_6"' in script, "TheRig clip uses wrong rig ID")
        require('Track("left_forearm"' in script, "evaluated limb track missing")
        require('.Matrix(' in script, "TheRig clip did not use exact matrix frames")
        rig_script = render_rig_definition(armature, animation_schema)
        require('AnimationRig("jdh:therig_2_6"' in rig_script, "rig definition ID drift")
        require(rig_script.count('Bone("') == len(result.model["bones"]), "rig definition bone count drift")
        require('Socket("main_hand", bone: "right_item")' in rig_script, "item socket missing")

        before = [sampled[name] for name in ("left_upper_arm", "left_forearm", "left_hand")]
        control_delta = 0.0
        for control_name in ("L_Arm_Target", "L_Arm_FK"):
            control = armature.pose.bones.get(control_name)
            if control is None:
                continue
            original_basis = control.matrix_basis.copy()
            control.location += Vector((0.12, 0.04, 0.03))
            control.rotation_mode = "XYZ"
            control.rotation_euler.x += 0.2
            bpy.context.view_layer.update()
            after = [
                sample_schema_pose_value(armature, animation_schema, name, None, True)[0]
                for name in ("left_upper_arm", "left_forearm", "left_hand")
            ]
            control_delta = max(
                control_delta,
                *(
                    abs(left - right)
                    for old, new in zip(before, after)
                    for left, right in zip(old.matrix, new.matrix)
                ),
            )
            control.matrix_basis = original_basis
            bpy.context.view_layer.update()
        require(control_delta > 1.0e-4, "evaluated rig controls did not reach exported limb tracks")

        by_name = {mesh["name"]: mesh for mesh in result.model["meshes"]}
        for mesh_name in ("Body", "Head", "L_Arm4x4", "R_Arm4x4", "L_Leg", "R_Leg"):
            vertices = by_name[mesh_name]["vertices"]
            require(vertices, f"{mesh_name} has no exported vertices")
            require(
                any(len(vertex["boneIndices"]) > 1 for vertex in vertices),
                f"{mesh_name} lost its soft lattice weights",
            )
            require(
                any(vertex["boneIndices"] != [0] for vertex in vertices),
                f"{mesh_name} was rigidly attached to root",
            )

        with tempfile.TemporaryDirectory(prefix="echo-therig-") as directory:
            path = write_skin_json(
                Path(directory)
                / "assets" / "jdh" / "echo_animation" / "models" / "therig_2_6",
                result.model,
            )
            loaded = json.loads(path.read_text(encoding="utf-8"))
            require(loaded["formatVersion"] == 2, "JSON format drift")
            require(len(loaded["bones"]) == len(result.model["bones"]), "JSON bone count drift")
            rig_path = write_rig_definition(path, armature, result.schema)
            expected = (
                Path(directory) / "data" / "jdh" / "echo_animation"
                / "animation_rigs" / "therig_2_6.animation-rig.es"
            )
            require(rig_path == expected, "rig definition resource path drift")
            require(rig_path.is_file(), "rig definition was not written")

        soft = sum(
            len(vertex["boneIndices"]) > 1
            for mesh in result.model["meshes"]
            for vertex in mesh["vertices"]
        )
        print(
            "THERIG_INTEGRATION_OK",
            f"meshes={len(result.model['meshes'])}",
            f"bones={len(result.model['bones'])}",
            f"vertices={result.vertex_count}",
            f"soft_vertices={soft}",
            f"warnings={len(result.warnings)}",
        )
    finally:
        addon.unregister()


if __name__ == "__main__":
    main()
