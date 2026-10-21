# Echo Animation Authoring

Dev-only Forge client mod `echo_animation_authoring` for editing Echo Animation resources in a development Minecraft instance.

Production modpack must not include this JAR.

Implemented:

- client-only mod entrypoint;
- Russian editor labels;
- client commands: `/echo_animation_authoring workspace`, `/echo_animation_authoring editor`, `/echo_animation_authoring face open [faceRigId]`, `/echo_animation_authoring migrate <legacyPath>`, `/echo_animation_authoring diagnostics [limit]`;
- workspace boundary validation;
- legacy facial migration output to EchoScript;
- neutral 64x64 PNG export;
- public `AnimationAuthoringService` draft API for editor integrations;
- keyframe mutation, undo/redo, autosave/recovery sidecar, discovery and preview hook operations;
- atomic `.animation-clip.es` save after structural validation and Echo Animation compiler probe;
- JAR scan proving no `io.github.kxyr0.echoanimation` API/runtime copies are bundled.

Final JAR: `build/libs/echo-animation-authoring-0.3.0-SNAPSHOT.jar`.
