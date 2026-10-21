from __future__ import annotations

import re


THERIG_SCHEMA_ID = "THERIG_2_6"
THERIG_RIG_ID = "jdh:therig_2_6"


# Historical compact profile kept below for migration/reference only. Exporter
# 3.x no longer collapses TheRig into this list; it derives the complete runtime
# deform hierarchy dynamically and bakes evaluated matrices.
THERIG_BONE_SPECS = (
    ("root", "Root2", None),
    ("body_lower", "Spine_Start3", "root"),
    ("body_mid", "Spine", "body_lower"),
    ("body_upper", "Spine_End2", "body_mid"),
    ("head", "Head", "body_upper"),
    ("chin", "Chin", "head"),

    ("left_eye_top_3", "L_Eye_Top3", "head"),
    ("left_eye_top_2", "L_Eye_Top2", "head"),
    ("left_eye_top_1", "L_Eye_Top1", "head"),
    ("left_eye_mid_2", "L_Eye_Mid2", "head"),
    ("left_eye_mid_1", "L_Eye_Mid1", "head"),
    ("left_eye_bottom_3", "L_Eye_Bot3", "head"),
    ("left_eye_bottom_2", "L_Eye_Bot2", "head"),
    ("left_eye_bottom_1", "L_Eye_Bot1", "head"),
    ("left_mouth_outer_top", "L_Mouth_Top2", "head"),
    ("right_eye_top_3", "R_Eye_Top3", "head"),
    ("right_eye_top_2", "R_Eye_Top2", "head"),
    ("right_eye_top_1", "R_Eye_Top1", "head"),
    ("right_eye_mid_2", "R_Eye_Mid2", "head"),
    ("right_eye_mid_1", "R_Eye_Mid1", "head"),
    ("right_eye_bottom_3", "R_Eye_Bot3", "head"),
    ("right_eye_bottom_2", "R_Eye_Bot2", "head"),
    ("right_eye_bottom_1", "R_Eye_Bot1", "head"),
    ("right_mouth_outer_top", "R_Mouth_Top2", "head"),
    ("left_eyebrow_base", "L_Eyebrow_BB", "head"),
    ("left_eyebrow_start", "L_Eyebrow_Start", "head"),
    ("right_eyebrow_base", "R_Eyebrow_BB", "head"),
    ("right_eyebrow_start", "R_Eyebrow_Start", "head"),
    ("right_mouth_top", "R_Mouth_Top1", "head"),
    ("mouth_top", "Mouth_Top", "head"),
    ("left_mouth_top", "L_Mouth_Top1", "head"),
    ("right_mouth_bottom", "R_Mouth_Bot1", "head"),
    ("mouth_bottom", "Mouth_Bot", "head"),
    ("left_mouth_bottom", "L_Mouth_Bot1", "head"),

    ("left_shoulder", "L_Shoulder", "body_upper"),
    ("left_upper_arm", "L_Arm", "left_shoulder"),
    ("left_forearm", "L_Arm_Bend", "left_upper_arm"),
    ("left_hand", "L_Wrist", "left_forearm"),
    ("right_shoulder", "R_Shoulder", "body_upper"),
    ("right_upper_arm", "R_Arm", "right_shoulder"),
    ("right_forearm", "R_Arm_Bend", "right_upper_arm"),
    ("right_hand", "R_Wrist", "right_forearm"),
    ("left_thigh", "L_Leg", "body_lower"),
    ("left_shin", "L_Leg_Bend", "left_thigh"),
    ("left_foot", "L_Ankle", "left_shin"),
    ("right_thigh", "R_Leg", "body_lower"),
    ("right_shin", "R_Leg_Bend", "right_thigh"),
    ("right_foot", "R_Ankle", "right_shin"),
)


_REQUIRED_SIGNATURE = {
    "Root2",
    "Spine_Start3",
    "Spine_End2",
    "L_Arm_Spline",
    "R_Arm_Spline",
    "L_Leg_Spline",
    "R_Leg_Spline",
}
_SPLINE = re.compile(r"^([LR])_(Arm|Leg)_Spline(?:\.(\d{3}))?$")


THERIG_SEMANTIC_RUNTIME_NAMES = {
    "Root": "rig_root",
    "Root2": "root",
    "Body": "body_root",
    "Body_Lower": "body_lower",
    "Body_Upper": "body",
    "Head_Control": "head_control",
    "Head": "head",
    "L_Shoulder": "left_shoulder",
    "L_Arm": "left_upper_arm",
    "L_Arm_Bend": "left_forearm",
    "L_Wrist": "left_hand",
    "L_Item": "left_item",
    "R_Shoulder": "right_shoulder",
    "R_Arm": "right_upper_arm",
    "R_Arm_Bend": "right_forearm",
    "R_Wrist": "right_hand",
    "R_Item": "right_item",
    "L_Leg": "left_thigh",
    "L_Leg_Bend": "left_shin",
    "L_Ankle": "left_foot",
    "R_Leg": "right_thigh",
    "R_Leg_Bend": "right_shin",
    "R_Ankle": "right_foot",
}

THERIG_EXPLICIT_RUNTIME_BONES = ("L_Item", "R_Item", "neutral_bone")


def therig_runtime_name(source_name: str) -> str:
    """Return a stable Echo BoneId for a TheRig source bone.

    Semantic attachment/body bones keep human-readable stable IDs; every other
    evaluated deform bone is preserved one-to-one with a deterministic sanitized
    source name. No authoring control semantics need to be reimplemented in Echo.
    """
    semantic = THERIG_SEMANTIC_RUNTIME_NAMES.get(source_name)
    if semantic is not None:
        return semantic
    value = source_name.strip().lower()
    value = re.sub(r"[^a-z0-9_/-]+", "_", value)
    value = re.sub(r"_+", "_", value).strip("_/-")
    if not value or not value[0].isalpha():
        value = "bone_" + value
    return value[:64]


def is_therig_2_6(armature) -> bool:
    data = getattr(armature, "data", armature)
    names = {bone.name for bone in getattr(data, "bones", ())}
    return _REQUIRED_SIGNATURE.issubset(names)


def _pair(first: str, second: str, amount: float) -> dict[str, float]:
    amount = min(1.0, max(0.0, amount))
    return {first: 1.0 - amount, second: amount}


def therig_weight_targets(group_name: str) -> dict[str, float]:
    """Legacy compact-profile weight mapping retained for old files/tests."""
    match = _SPLINE.fullmatch(group_name)
    if match:
        side = "left" if match.group(1) == "L" else "right"
        limb = match.group(2)
        progress = min(1.0, max(0.0, int(match.group(3) or 0) / 11.0))
        if limb == "Arm":
            proximal, middle, distal = (
                f"{side}_upper_arm",
                f"{side}_forearm",
                f"{side}_hand",
            )
        else:
            proximal, middle, distal = (
                f"{side}_thigh",
                f"{side}_shin",
                f"{side}_foot",
            )
        if progress <= 0.5:
            return _pair(proximal, middle, progress * 2.0)
        return _pair(middle, distal, progress * 2.0 - 1.0)

    tweak = re.fullmatch(r"([LR])_(Arm|Leg)_Tweak6", group_name)
    if tweak:
        side = "left" if tweak.group(1) == "L" else "right"
        return {
            f"{side}_{'hand' if tweak.group(2) == 'Arm' else 'foot'}": 1.0
        }

    collapsed = {
        "L_Mouth_Top2": {"left_mouth_top": 1.0},
        "R_Mouth_Top2": {"right_mouth_top": 1.0},
        "L_Mouth_Bot2": {"left_mouth_bottom": 1.0},
        "R_Mouth_Bot2": {"right_mouth_bottom": 1.0},
        "L_Mouth_Mid": {"left_mouth_top": 0.5, "left_mouth_bottom": 0.5},
        "R_Mouth_Mid": {"right_mouth_top": 0.5, "right_mouth_bottom": 0.5},
        "L_Mouth": {"left_mouth_top": 0.5, "left_mouth_bottom": 0.5},
        "R_Mouth": {"right_mouth_top": 0.5, "right_mouth_bottom": 0.5},
        "Mid_Mouth": {"mouth_top": 0.5, "mouth_bottom": 0.5},
        "Eyelash": {"head": 1.0},
    }
    if group_name in collapsed:
        return collapsed[group_name]

    if re.match(r"^L_(Thumb|Index|Middle|Ring|Pinky|Fingers)", group_name):
        return {"left_hand": 1.0}
    if re.match(r"^R_(Thumb|Index|Middle|Ring|Pinky|Fingers)", group_name):
        return {"right_hand": 1.0}
    if "Breast" in group_name:
        return {"body_upper": 1.0}
    if any(token in group_name for token in ("Eye", "Eyebrow", "Mouth", "Lip")):
        return {"head": 1.0}
    return {}


def therig_lattice_fallback(lattice_name: str) -> str:
    if lattice_name.startswith("Head"):
        return "head"
    if lattice_name.startswith("Body"):
        return "body"
    if lattice_name.startswith("L_Arm"):
        return "left_upper_arm"
    if lattice_name.startswith("R_Arm"):
        return "right_upper_arm"
    if lattice_name.startswith("L_Leg"):
        return "left_thigh"
    if lattice_name.startswith("R_Leg"):
        return "right_thigh"
    if "Breast" in lattice_name:
        return "body"
    return "root"
