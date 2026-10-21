# Implicit Dialogue Audience

Status: completed.

## Goal

Allow normal player-scoped Story authoring to call `Dialogue.Load("namespace:id")` without duplicating a technical
`audienceId` in content.

## Boundary

- Story EchoScript lowers `Dialogue.Load` with required `dialogueId` and optional `audienceId`.
- Story core and contracts remain dialogue-agnostic.
- The independent Story-Dialogue adapter resolves the current player from the stable `/player/<uuid>` execution-id
  segment, then falls back to the only online player for local/single-player execution.
- An explicit `audienceId` remains an advanced multiplayer override.

## Verification

- Compiler test proves omitted `audienceId` is not emitted into the external-action payload.
- Adapter test proves a player UUID is inherited from the Story execution ID.
- Story and adapter builds must pass before deployment.
