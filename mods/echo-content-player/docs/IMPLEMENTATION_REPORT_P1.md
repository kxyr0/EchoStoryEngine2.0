# P1 implementation report

Дата реализации: 2026-07-25.

## Modules

- `player-contracts`: typed immutable public API: `EchoPlayerId`, display name, Spark, skin selection, profile snapshot, lifecycle metadata, diagnostics and mutation id.
- `player-core`: validators, Spark mutations, EchoScript template compiler/binder, repository boundary, lifecycle checkpoints, mutation ledger, persistence DTO, recovery and synchronization decisions.
- `player-forge-api-1.20.1`: public query/mutation/template/lifecycle/appearance interfaces.
- `player-forge-1.20.1`: Forge mod entrypoint, login/logout hooks, command registration, sync packet/channel, client mirror, SavedData host, custom skin resource resolution and reload plan DTO.
- `player-testmod-1.20.1`: development-only hooks; not embedded into production jar.

## Working profile

Default profile template:

```text
EchoPlayerId = account UUID
account name = GameProfile name
resolved display name = account name
name mode = ACCOUNT
Spark = 100/100
skin mode = ACCOUNT
SkinId = empty
model = vanilla metadata
template ID = jdh:player/default
profile revision = 1 on first creation
appearance revision = 0
```

## Persistence

`PlayerProfileData` and `PlayerPersistenceCodec` cover restart DTO round trip in unit tests. Forge `PlayerProfileSavedData` hosts encoded profile records, and `SavedDataPlayerProfileRepository` is opened on login/logout so server lifecycle uses the world-backed repository behind the same repository boundary.

## Mutation evidence

`PlayerMutationLedgerTest` proves:

- same mutation id and same request returns same result;
- mutation body is not executed twice;
- same mutation id with different args returns conflict;
- duplicate mutation count stays zero.

## Hot reload

Template compilation uses the real EchoScript parser plus binder. Broken templates return bounded diagnostics. Skin validation checks PNG signature, decode and strict 64x64 dimensions, then plans previous-known-good/account fallback.

## Dependency scans

Architecture tests and Gradle `verifyForbiddenText` scan production source for forbidden Echo engine coupling. `player-contracts` also proves Minecraft/Forge/EchoScript imports = 0.

## Source shape

Gradle `sourceShapeCheck` enforces:

- Java classes over 200 logical LOC = 0;
- EchoScript files over 200 logical LOC = 0;
- leaf Java packages over 7 production files = 0;
- leaf content folders over 7 scripts = 0.

## Tests

Automated coverage includes:

- ID stability;
- account rename semantics;
- display name validation;
- Spark operations;
- skin ID validation;
- skin PNG validation and fallback plan;
- template parser/binder;
- lifecycle login preservation;
- mutation ledger;
- persistence DTO round trip;
- architecture boundary scans.

Manual Minecraft acceptance has not been executed in this coding pass.
