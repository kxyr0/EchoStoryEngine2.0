# Segmented Humanoid

A2 renders a standard Minecraft `64x64` skin as a segmented humanoid. The PNG is
not cut on disk; the client model uses UV regions from the original atlas.

Visible segments are:

- `head`
- `body`
- `left_upper_arm`
- `left_forearm`
- `right_upper_arm`
- `right_forearm`
- `left_thigh`
- `left_shin`
- `right_thigh`
- `right_shin`

Each vanilla 12 px arm or leg is split into a 6 px upper segment and a 6 px
lower segment. Standard overlays move with the same segment: hat with head,
jacket with body, sleeves with arm halves, and pants with leg halves.

The generic render boundary receives `AnimationSubjectId`, skin texture,
`CLASSIC` or `SLIM`, `PoseStack`, buffers, packed light, overlay, and partial
tick. Echo Animation does not import Echo NPC implementation classes.

For socket visual acceptance, launch the client with
`-Decho_animation.debug.main_hand_socket_axes=true`. This draws only the
`main_hand` axes; it does not render held items.

## Manual A2 Acceptance

Spawn the NPC with a stable `ActorId`, then drive the neutral animation subject
with the same id:

```mcfunction
/echo_npc spawn jdh:john jdh:john_father ~ ~ ~
/echo_animation body_rigs
/echo_animation body_clips
/echo_animation acceptance state jdh:john acting
/echo_animation acceptance state jdh:john head
/echo_animation acceptance state jdh:john body
/echo_animation acceptance state jdh:john left_arm
/echo_animation acceptance state jdh:john right_arm
/echo_animation acceptance state jdh:john both_arms
/echo_animation acceptance state jdh:john left_leg
/echo_animation acceptance state jdh:john right_leg
/echo_animation acceptance state jdh:john step
/echo_animation acceptance state jdh:john wave
/echo_animation acceptance state jdh:john knee
/echo_animation acceptance state jdh:john idle
/echo_animation acceptance remove jdh:john
```

The A2 acceptance graph uses `jdh:humanoid`, `jdh:humanoid_idle`,
`jdh:humanoid_test_acting`, `jdh:humanoid_test_wave`,
`jdh:humanoid_test_knee`, and the `humanoid_test_*` clips from the external
`story/content-pack/jdh/echo_animation` authoring workspace. Use `acting` as
the main full-body tuning loop, and use the narrower bone-check states only
when a specific segment needs isolation.
