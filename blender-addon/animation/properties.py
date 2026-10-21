from __future__ import annotations

import bpy
from bpy.props import BoolProperty, CollectionProperty, EnumProperty, FloatProperty, IntProperty, PointerProperty, StringProperty

from .presets import items as preset_items


ANIMATION_TYPES = (
    ("ONE_SHOT", "One Shot", "Play the complete Action once"),
    ("LOOP", "Loop", "Repeat the complete Action or a loop range"),
    ("STATEFUL_LOOP", "Stateful Loop", "Optional Enter and Exit around a looping state"),
    ("STATEFUL_HOLD", "Stateful Hold", "Enter a single held pose and optionally Exit"),
    ("LOCOMOTION", "Locomotion", "Walk, run, sneak or crawl with typed foot contacts"),
    ("ONE_SHOT_HOLD", "One Shot + Hold", "Play once and keep the last frame"),
    ("ADVANCED", "Advanced", "Manual control over segments, points, events and transitions"),
)


def _dirty(self, _context):
    owner = getattr(self, "id_data", None)
    if owner is not None:
        owner["echo_validation_dirty"] = True


class EchoMarkerItem(bpy.types.PropertyGroup):
    marker_id: StringProperty(name="Marker ID")
    kind: EnumProperty(items=(("EVENT", "Event", "One-time runtime event"), ("POINT", "Point", "Safe transition point"), ("LEFT_FOOT", "Left Foot", "Left foot contact"), ("RIGHT_FOOT", "Right Foot", "Right foot contact"), ("HOLD", "Hold", "Held pose")))
    name: StringProperty(name="Name", default="point", update=_dirty)
    frame: IntProperty(name="Frame", update=_dirty)
    payload: StringProperty(name="Payload", update=_dirty)
    segment: StringProperty(name="Segment", update=_dirty)


class EchoAnimationMetadata(bpy.types.PropertyGroup):
    enabled: BoolProperty(name="Echo Animation Enabled", default=False, update=_dirty)
    resource_id: StringProperty(name="Resource ID", default="jdh:animation", update=_dirty)
    rig_id: StringProperty(name="Rig", default="jdh:humanoid", update=_dirty)
    animation_type: EnumProperty(name="Animation Type", items=ANIMATION_TYPES, default="ONE_SHOT", update=_dirty)
    preset: EnumProperty(name="Preset", items=preset_items(), default="ONE_SHOT")
    channel: EnumProperty(name="Channel", items=(("BODY", "Body", "Full body"), ("UPPER_BODY", "Upper Body", "Torso, head and arms"), ("LOWER_BODY", "Lower Body", "Root and legs"), ("HEAD", "Head", "Head only"), ("FACE", "Face", "Facial channel"), ("CUSTOM", "Custom", "Comma-separated bone mask")), default="BODY", update=_dirty)
    bone_mask: StringProperty(name="Bone Mask", default="BODY", update=_dirty)
    custom_bones: StringProperty(name="Custom Bones", description="Comma-separated exported runtime bone names", update=_dirty)
    source_state: StringProperty(name="Source State", default="idle", update=_dirty)
    active_state: StringProperty(name="Active State", default="active", update=_dirty)
    target_state: StringProperty(name="Target State", default="idle", update=_dirty)
    return_state: StringProperty(name="Return State", default="idle", update=_dirty)
    enter_segment_name: StringProperty(name="Enter Segment", default="", update=_dirty)
    loop_segment_name: StringProperty(name="Loop Segment", default="", update=_dirty)
    exit_segment_name: StringProperty(name="Exit Segment", default="", update=_dirty)
    hold_point_name: StringProperty(name="Hold Point", default="hold_pose", update=_dirty)
    enter_enabled: BoolProperty(name="Enter", default=False, update=_dirty)
    loop_enabled: BoolProperty(name="Loop", default=False, update=_dirty)
    exit_enabled: BoolProperty(name="Exit", default=False, update=_dirty)
    blend_in_ticks: IntProperty(name="Blend In", default=3, min=0, update=_dirty)
    blend_out_ticks: IntProperty(name="Blend Out", default=3, min=0, update=_dirty)
    fallback_blend_ticks: IntProperty(name="Fallback Blend", default=3, min=0, update=_dirty)
    root_motion_mode: EnumProperty(name="Root Motion", items=(("DISABLED", "Disabled", "Reject character displacement and keep the NPC stationary"), ("EXTRACT", "Extract", "Export root movement metadata separately from local pose"), ("PRESERVE", "Preserve", "Keep root movement in the regular root bone track")), default="DISABLED", update=_dirty)
    root_bone: StringProperty(name="Root Bone", default="root", update=_dirty)
    authored_speed: FloatProperty(name="Authored Speed", description="Locomotion speed in blocks per second", default=0.0, min=0.0, precision=3, update=_dirty)
    hold_last_frame: BoolProperty(name="Hold Last Frame", default=False, update=_dirty)
    export_animation_resource: BoolProperty(name="Export Animation Resource", default=True, update=_dirty)
    output_directory: StringProperty(name="Output Directory", subtype="DIR_PATH", update=_dirty)
    resource_folder: StringProperty(name="Resource Folder", default="chapter_1/family", update=_dirty)
    overwrite_policy: EnumProperty(name="Overwrite Policy", items=(("GENERATED", "Overwrite Generated Files", "Overwrite only files carrying the Echo generated header"), ("ASK", "Ask Before Overwrite", "Require explicit confirmation for an existing file"), ("NEVER", "Never Overwrite Existing", "Skip every existing file")), default="GENERATED")
    frame_start: IntProperty(name="Action Start", default=1, update=_dirty)
    frame_end: IntProperty(name="Action End", default=30, update=_dirty)
    engine_tick_rate: FloatProperty(name="Engine Tick Rate", default=20.0, min=1.0, update=_dirty)
    duration_ticks: IntProperty(name="Duration Ticks", description="0 calculates duration from Blender FPS", default=0, min=0, update=_dirty)
    sample_step: IntProperty(name="Sample Every", default=1, min=1, max=100, update=_dirty)
    interpolation: EnumProperty(name="Interpolation", items=tuple((value, value.title(), "") for value in ("STEP", "LINEAR", "SMOOTH", "CUBIC")), default="LINEAR", update=_dirty)
    loop_seam: EnumProperty(name="Loop Seam", items=tuple((value, value.title(), "") for value in ("HOLD", "LINEAR", "SMOOTH")), default="SMOOTH", update=_dirty)
    position_tolerance: FloatProperty(name="Position Tolerance", default=0.01, min=0.0)
    rotation_tolerance: FloatProperty(name="Rotation Tolerance", default=1.0, min=0.0)
    scale_tolerance: FloatProperty(name="Scale Tolerance", default=0.01, min=0.0)
    export_scale: BoolProperty(name="Export Scale", default=True, update=_dirty)
    optimize_holds: BoolProperty(name="Remove Redundant Holds", default=True, update=_dirty)
    boundary_key_mode: EnumProperty(name="Boundary Keys", items=(("ANIMATED", "Only Animated Bones", "Insert keys only for bones already animated"), ("ALL", "All Exported Bones", "Insert keys for every bone included by the mask")), default="ANIMATED")
    markers: CollectionProperty(type=EchoMarkerItem)
    marker_index: IntProperty(default=0)
    validation_status: EnumProperty(items=(("NONE", "Not Initialized", ""), ("VALID", "Valid", ""), ("WARNING", "Warning", ""), ("INVALID", "Invalid", "")), default="NONE")
    validation_summary: StringProperty(default="Not validated")
    validation_details: StringProperty(default="")


class EchoAnimationDraft(bpy.types.PropertyGroup):
    action_name: StringProperty(name="Action", default="new_animation")
    resource_id: StringProperty(name="Resource ID", default="jdh:new_animation")
    rig_id: StringProperty(name="Rig", default="jdh:humanoid")
    animation_type: EnumProperty(name="Animation Type", items=ANIMATION_TYPES, default="ONE_SHOT")
    preset: EnumProperty(name="Preset", items=preset_items(), default="ONE_SHOT")
    frame_start: IntProperty(name="Start", default=1)
    frame_end: IntProperty(name="End", default=30)


class EchoMigrationSettings(bpy.types.PropertyGroup):
    enter_action: PointerProperty(name="Enter Action", type=bpy.types.Action)
    loop_action: PointerProperty(name="Loop Action", type=bpy.types.Action)
    exit_action: PointerProperty(name="Exit Action", type=bpy.types.Action)
    new_name: StringProperty(name="New Action", default="humanoid_run")
    source_state: StringProperty(name="Source State", default="idle")
    active_state: StringProperty(name="Active State", default="run")
    target_state: StringProperty(name="Target State", default="idle")
    preview_summary: StringProperty(default="Choose source Actions")


PROPERTY_CLASSES = (EchoMarkerItem, EchoAnimationMetadata, EchoAnimationDraft, EchoMigrationSettings)


def register_action_properties() -> None:
    bpy.types.Action.echo_animation = PointerProperty(type=EchoAnimationMetadata)
    bpy.types.WindowManager.echo_animation_draft = PointerProperty(type=EchoAnimationDraft)
    bpy.types.WindowManager.echo_animation_migration = PointerProperty(type=EchoMigrationSettings)


def unregister_action_properties() -> None:
    for owner, name in ((bpy.types.WindowManager, "echo_animation_migration"), (bpy.types.WindowManager, "echo_animation_draft"), (bpy.types.Action, "echo_animation")):
        if hasattr(owner, name):
            delattr(owner, name)
