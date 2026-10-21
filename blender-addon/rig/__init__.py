"""Shared rig schema and coordinate contracts."""

from .schema import (
    LEGACY_BONE_ORDER,
    LEGACY_REST_LOCAL_POSITION,
    LEGACY_RIG_ID,
    BoneSchemaEntry,
    SkeletonSchema,
    resolve_skeleton_schema,
)
from .therig import THERIG_RIG_ID, THERIG_SCHEMA_ID

__all__ = (
    "LEGACY_BONE_ORDER",
    "LEGACY_REST_LOCAL_POSITION",
    "LEGACY_RIG_ID",
    "BoneSchemaEntry",
    "SkeletonSchema",
    "THERIG_RIG_ID",
    "THERIG_SCHEMA_ID",
    "resolve_skeleton_schema",
)
