# Progress

## A0/A1 status

Done:

- created independent `echo-animation-authoring` repository;
- configured Forge 1.20.1 / Java 17 client mod;
- authoring compiles against public Maven artifacts Echo Animation;
- implemented workspace guard;
- added in-game screen `/echo_animation_authoring editor` and `/echo_animation_authoring face open [faceRigId]`;
- added client commands `/echo_animation_authoring workspace`, migrate and diagnostics;
- implemented deterministic legacy migration and pixel-exact neutral skin export;
- added public draft API for editor integrations: keyframe mutation, save, hot reload hook, undo/redo,
  autosave/recovery sidecar, discovery and preview hook;
- added authoring plain-JVM tests and authoring JAR content verification.

Pending:

- real content-pack workspace picker and persisted editor session beyond JVM property `echoAnimationAuthoring.workspace`;
- manual visual acceptance in Minecraft client.
