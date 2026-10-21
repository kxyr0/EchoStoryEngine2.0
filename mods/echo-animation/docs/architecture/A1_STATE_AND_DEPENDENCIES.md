# A1 State And Dependencies

## Hard Dependency Rule

`compileOnly` without embedding still creates compile-time coupling when it
points at another mod runtime JAR. A1 removes the Echo Animation dependency on
the Echo NPC runtime JAR instead of replacing it with reflection.

Echo Animation may publish public contracts and public Forge API artifacts. It
must not import Echo NPC implementation packages or encode NPC-specific renderer
behavior.

## Optional Dialogue Relationship

Echo Animation has no Gradle dependency on Echo Dialogue and no Java imports
from Echo Dialogue. Current optional Dialogue integration is a Forge-event
bridge discovered at runtime by `DialogueVisualFaceLayerBridge`.

That bridge still contains concrete reflective knowledge of:

- `io.github.kxyr0.echodialogue.forge.api.EchoDialogueSpeakerVisualRenderEvent`;
- accessor method names used by the event reader.

This is soft semantic coupling, not a hard compile-time dependency.

## AnimationSubjectId

`AnimationSubjectId` is a neutral logical subject ID under
`io.github.kxyr0.echoanimation.api.subject`. It is distinct from NPC identity.
The NPC adapter maps public `ActorId.value()` into this neutral ID.

## Graph And Channels

A graph is shared immutable definition data. A subject owns runtime state:

- pinned graph revision;
- independent channel slots;
- local logical tick positions;
- waiting-for-exit flags;
- active ownership leases.

Changing one channel does not reset another channel. Two subjects can share one
graph without sharing runtime state.

## Playback And Transitions

Clip completion is not state completion. `HOLD_LAST_FRAME` clamps at the final
sample and stays in state. `WAIT_FOR_EXIT` requires an explicit event. Matching
transitions are selected by authored order; duplicate best order is ambiguous
and leaves state unchanged.

## Hot Reload

A1 establishes the model for graph revision pinning: existing subjects stay on
their current graph revision, new subjects receive the newest valid revision,
and old revisions are releasable after their final subject is removed. Full
`.animation-graph.es` parsing and exact incremental parser-count proof remain
limited in A1.
