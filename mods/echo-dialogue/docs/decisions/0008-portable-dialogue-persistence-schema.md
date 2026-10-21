# ADR 0008: Portable Dialogue Persistence Schema

Status: accepted for D2.

Durable dialogue snapshots use a dedicated `dialogue-persistence` module and schema V1. Core owns portable checkpoint contracts and compiled fingerprinting, while file paths, JSON codec and atomic file behavior stay outside core.

Reason:

- core must remain independent from Forge, Minecraft, Gson, `Path` and filesystem APIs;
- recovery must compare compiled meaning, not raw source formatting or resource path;
- schema validation and repository results need explicit diagnostics instead of exceptions or booleans.

Consequences:

- persisted fields are limited to session identity, dialogue identity, compiled fingerprint, audience, phase, revision, cursor, timestamps, terminal fields, bounded metadata and repository revision;
- raw source, client UI state, packets, callbacks, stack traces and full diagnostic history are excluded;
- file repository keeps one primary, one backup and one temp file per session;
- corrupt primary can fall back to backup, but corrupt primary plus corrupt backup is an explicit failed load.
