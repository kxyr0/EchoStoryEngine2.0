from __future__ import annotations

import json
import math
import sys
from collections import Counter, defaultdict
from pathlib import Path

import bpy


def matrix_values(matrix):
    return [float(matrix[row][column]) for row in range(4) for column in range(4)]


def custom_properties(owner):
    return {
        key: owner[key]
        for key in owner.keys()
        if key != "_RNA_UI" and isinstance(owner[key], (str, int, float, bool))
    }


def modifier_info(modifier):
    result = {"name": modifier.name, "type": modifier.type, "show_viewport": modifier.show_viewport}
    target = getattr(modifier, "object", None)
    if target is not None:
        result["object"] = target.name
    return result


def constraint_info(constraint):
    result = {
        "name": constraint.name,
        "type": constraint.type,
        "influence": float(constraint.influence),
        "mute": bool(constraint.mute),
    }
    for attribute in (
        "subtarget",
        "pole_subtarget",
        "chain_count",
        "use_tail",
        "use_rotation",
        "use_location",
        "owner_space",
        "target_space",
    ):
        if hasattr(constraint, attribute):
            value = getattr(constraint, attribute)
            if isinstance(value, (str, int, float, bool)):
                result[attribute] = value
    target = getattr(constraint, "target", None)
    pole_target = getattr(constraint, "pole_target", None)
    if target is not None:
        result["target"] = target.name
    if pole_target is not None:
        result["pole_target"] = pole_target.name
    return result


def armature_info(obj):
    collection_members = defaultdict(list)
    for bone in obj.data.bones:
        for collection in bone.collections:
            collection_members[collection.name].append(bone.name)

    bones = []
    for bone in obj.data.bones:
        pose_bone = obj.pose.bones.get(bone.name)
        bones.append(
            {
                "name": bone.name,
                "parent": bone.parent.name if bone.parent else None,
                "use_deform": bool(bone.use_deform),
                "use_connect": bool(bone.use_connect),
                "collections": sorted(collection.name for collection in bone.collections),
                "head_local": list(bone.head_local),
                "tail_local": list(bone.tail_local),
                "length": float(bone.length),
                "matrix_local": matrix_values(bone.matrix_local),
                "properties": custom_properties(bone),
                "constraints": [constraint_info(value) for value in pose_bone.constraints] if pose_bone else [],
                "pose_properties": custom_properties(pose_bone) if pose_bone else {},
            }
        )

    return {
        "name": obj.name,
        "bone_count": len(bones),
        "deform_bone_count": sum(bone["use_deform"] for bone in bones),
        "properties": custom_properties(obj),
        "data_properties": custom_properties(obj.data),
        "collections": {
            name: sorted(members)
            for name, members in sorted(collection_members.items())
        },
        "bones": bones,
    }


def mesh_info(obj):
    group_stats = []
    for group in obj.vertex_groups:
        weighted = 0
        total = 0.0
        minimum = math.inf
        maximum = 0.0
        for vertex in obj.data.vertices:
            assignment = next((value for value in vertex.groups if value.group == group.index), None)
            if assignment is None or assignment.weight <= 0.0:
                continue
            weighted += 1
            total += float(assignment.weight)
            minimum = min(minimum, float(assignment.weight))
            maximum = max(maximum, float(assignment.weight))
        group_stats.append(
            {
                "name": group.name,
                "vertices": weighted,
                "min": 0.0 if weighted == 0 else minimum,
                "max": maximum,
                "average": 0.0 if weighted == 0 else total / weighted,
            }
        )

    influence_histogram = Counter()
    for vertex in obj.data.vertices:
        influence_histogram[sum(assignment.weight > 0.0 for assignment in vertex.groups)] += 1

    return {
        "name": obj.name,
        "vertices": len(obj.data.vertices),
        "polygons": len(obj.data.polygons),
        "loops": len(obj.data.loops),
        "uv_layers": [layer.name for layer in obj.data.uv_layers],
        "materials": [slot.material.name if slot.material else None for slot in obj.material_slots],
        "modifiers": [modifier_info(modifier) for modifier in obj.modifiers],
        "properties": custom_properties(obj),
        "vertex_groups": group_stats,
        "influence_histogram": dict(sorted(influence_histogram.items())),
    }


def lattice_info(obj):
    points = []
    for index, point in enumerate(obj.data.points):
        points.append(
            {
                "index": index,
                "co": list(point.co),
                "co_deform": list(point.co_deform),
                "groups": [
                    {"name": obj.vertex_groups[value.group].name, "weight": float(value.weight)}
                    for value in point.groups
                    if value.weight > 0.0
                ],
            }
        )
    return {
        "name": obj.name,
        "dimensions": [obj.data.points_u, obj.data.points_v, obj.data.points_w],
        "interpolation": [
            obj.data.interpolation_type_u,
            obj.data.interpolation_type_v,
            obj.data.interpolation_type_w,
        ],
        "matrix_world": matrix_values(obj.matrix_world),
        "modifiers": [modifier_info(modifier) for modifier in obj.modifiers],
        "points": points,
    }


def action_info(action):
    bone_channels = Counter()
    other_channels = []
    slots = getattr(action, "slots", ())
    for slot in slots:
        channelbag = action.layers[0].strips[0].channelbag(slot) if action.layers and action.layers[0].strips else None
        fcurves = channelbag.fcurves if channelbag else ()
        for curve in fcurves:
            path = curve.data_path
            if path.startswith('pose.bones["'):
                bone_channels[path.split('"', 2)[1]] += 1
            else:
                other_channels.append(path)
    return {
        "name": action.name,
        "frame_range": list(action.frame_range),
        "bone_channels": dict(sorted(bone_channels.items())),
        "other_channels": sorted(set(other_channels)),
    }


def main():
    output = Path(sys.argv[sys.argv.index("--") + 1])
    report = {
        "file": bpy.data.filepath,
        "blender": bpy.app.version_string,
        "objects": [
            {
                "name": obj.name,
                "type": obj.type,
                "parent": obj.parent.name if obj.parent else None,
                "parent_type": obj.parent_type,
                "parent_bone": obj.parent_bone,
                "visible": obj.visible_get(),
                "hide_viewport": obj.hide_viewport,
                "hide_render": obj.hide_render,
                "collections": [collection.name for collection in obj.users_collection],
            }
            for obj in bpy.data.objects
        ],
        "armatures": [armature_info(obj) for obj in bpy.data.objects if obj.type == "ARMATURE"],
        "lattices": [lattice_info(obj) for obj in bpy.data.objects if obj.type == "LATTICE"],
        "meshes": [mesh_info(obj) for obj in bpy.data.objects if obj.type == "MESH"],
        "actions": [action_info(action) for action in bpy.data.actions],
    }
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print("THERIG_AUDIT_WRITTEN", output, output.stat().st_size)


main()
