# Visibility Watch authoring

See the complete NPC and box resources in `examples/content-pack`. A watch is
Init-only and can select `ACTIVE_CAMERA` (default) or `PLAYER_EYES`.

`OUTSIDE_FRUSTUM` uses geometry only. `OUTSIDE_FRUSTUM_OR_OCCLUDED` additionally
supports `VISUAL_MULTI_RAY`. NPC samples include head/chest/pelvis/feet and AABB
corners; boxes include center, corners and face centers. `RequireVisibleFirst()`
prevents an initially hidden target from firing. `LostAfter` and `SeenAfter`
provide independent rendered-frame hysteresis.

Only a concrete NPC may use `HIDE_NPC_IMMEDIATELY`. A box watch opens an exact
blind-window event and may be rechecked server-side with
`assert_out_of_view`; it never attempts generic block concealment.

In a linear StoryResource method the runtime surface is:

```echoscript
View.Arm("jdh:father_vanish");
View.WaitUntilOutOfView("jdh:father_vanish");
View.AssertStillOutOfView("jdh:father_vanish");
View.Disarm("jdh:father_vanish");
```

`View.IsOutOfView` and `View.IsVisible` bind their boolean result to
`visibility.isOutOfView` and `visibility.isVisible`; `output: "name"` selects
a different Story variable. Calls use the current inherited PlayerRef.
