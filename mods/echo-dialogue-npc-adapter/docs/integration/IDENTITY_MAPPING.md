# Identity mapping

N4 mapping is exact:

```text
SpeakerId canonical string == ActorId canonical string
```

Example:

```text
SpeakerId: jdh:john_father
ActorId:   jdh:john_father
```

The adapter must not derive actor identity from:

- alias;
- display name;
- `NpcDefinitionId`;
- Minecraft entity UUID;
- file path;
- class name;
- suffix or prefix registry.

If conversion to `ActorId` is invalid, provider returns `NOT_HANDLED`.
Ordinary non-NPC speakers must not produce diagnostics.
