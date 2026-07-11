# Persistence And Versioning

## Policy

Persistence is a port. Domain and application code must not know whether state is stored in NBT, SavedData, JSON,
files, a database, or another backend.

The current port is `ExecutionStateRepository` in `story-core`.

## Versioning

Persistence design must account for:

- save schema version;
- story format version;
- story revision;
- stable node ids;
- migration chain;
- atomic write;
- recovery after interrupted write;
- backup policy;
- incompatible revision diagnostics;
- unknown fields policy;
- external action recovery.

Current version values are centralized in `gradle.properties`.

## Current Scaffold

`story-forge-1.20.1` provides `VolatileExecutionStateRepository`. It stores execution state for the server lifetime
only and proves the boundary. It is not durable production persistence.

## Forbidden Coupling

- Do not use array position as persistent node identity.
- Do not bind persisted models directly to public domain classes.
- Do not add serialization annotations to domain objects.
- Do not import NBT, Gson, Codec, SavedData, filesystem, or database APIs into contracts/core.
