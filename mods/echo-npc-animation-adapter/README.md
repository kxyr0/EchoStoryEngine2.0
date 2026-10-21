# Echo NPC Animation Adapter

Independent Forge 1.20.1 adapter between public Echo NPC APIs and public Echo
Animation APIs.

The adapter owns only:

- `ActorId` to `AnimationSubjectId` mapping;
- registration through `NpcClientRenderRegistrationEvent`;
- current A0 face-layer render bridge;
- isolated JAR verification;
- navigation locomotion playback with distinct `jdh:humanoid_walk` and `jdh:humanoid_run` animations;
- optional measured-speed to animation-playback synchronization for walk and sprint.

It does not import `EchoNpcEntity`, `EchoNpcEntityRenderer`,
`AnimationCatalogState` or other implementation classes.

[История изменений](docs/changelogs/README.md)
