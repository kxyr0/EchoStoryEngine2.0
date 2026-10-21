Echo Animation Runtime 0.7.1 system-sit + split-body source patch

Canonical JDH humanoid hierarchy:
root -> body_lower -> body (upper/chest) -> head/shoulders

0.7.1 changes:
- Vanilla hardcoded sitting arm/leg rotations are removed from SegmentedHumanoidModelRenderer.
- When AnimationHumanoidRenderContext.sitting() is true, HumanoidRenderDispatcher uses jdh:pose_sit_main as the base pose.
- Fallback clip ID jdh:pose_sit is accepted for older resources.
- Active/previous animation clips are layered only on bones they actually author, so an upper-body dialogue animation can play while the legs stay in pose_sit.
- Legacy schema 1/2 body tracks are mapped to body_lower when composing over the split-body rig.
- Missing pose_sit logs one warning and does NOT silently re-apply the vanilla pose.

Includes the 0.7.0 split-body changes:
- 14-bone canonical segmented humanoid.
- Runtime renderer torso split into two 6px skin sections.
- Nested pose/head/attachment transforms.
- body_lower included in upper-body masks.
- Clip schema 1/2 compatibility; schema 3+ uses explicit body_lower + body semantics.

This archive contains source changes only. The ready Forge jar is distributed separately.
