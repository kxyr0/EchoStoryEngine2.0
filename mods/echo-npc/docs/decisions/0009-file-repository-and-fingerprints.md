# 0009 File Repository And Fingerprints

## Status

Accepted for N2.

## Context

Recovery must be deterministic and must not guess when resources changed.

## Decision

Actor Save Schema V1 stores exact `NpcDefinitionFingerprint`. The file
repository writes UTF-8 JSON with temp/backup/replace policy and uses explicit
revision conflicts.

## Consequences

- Missing or fingerprint-mismatched definitions block recovery instead of
  migrating silently.
- Corrupt primary may fall back to a valid backup.
- Valid ABSENT primary prevents active backup resurrection after despawn.
