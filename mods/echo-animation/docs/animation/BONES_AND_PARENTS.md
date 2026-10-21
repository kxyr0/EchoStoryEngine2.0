# Bones and parents

`BoneId` is a bounded local ID, for example `face_root` or `upper_eyelids`.

Rules:

- exactly one authored ID per bone;
- missing parent is rejected;
- self-parent is rejected;
- direct and indirect cycles are rejected;
- hierarchy depth is bounded;
- authored order does not need to be parent-before-child.

Pose and animation deltas may target existing bones only and cannot change parent relationships.



## Segmented humanoid torso

The canonical segmented humanoid uses a two-bone torso:

```text
root
└─ body_lower
   └─ body          # upper torso / chest
      ├─ head
      ├─ left_shoulder
      └─ right_shoulder
```

`body` intentionally keeps its historical runtime id, but its meaning changes in clip schema 3+: it is the
upper torso / chest. `body_lower` is the lower torso / hips.

For backward compatibility, canonical `jdh:humanoid` clips with schema 1/2 that contain `body` but no
`body_lower` are sampled with that old whole-torso track remapped to `body_lower`. Because the new `body`
is its child, old animations still move the complete torso, head and shoulders as before. Schema 3+ never
uses this compatibility remap and can animate `body_lower` and `body` independently.
