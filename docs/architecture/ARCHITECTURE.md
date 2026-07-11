# Architecture

## Style

Echo Story Engine combines Clean Architecture, Ports and Adapters, Functional Core / Imperative Shell,
Dependency Inversion, explicit serializable state machines, and server-authoritative execution.

These names are not folder names by themselves. A package exists only when it has a current, testable purpose.

## Modules

```text
story-contracts <- story-core <- story-forge-1.20.1
```

`story-contracts` is a Java 17 public contract module. It exposes identifiers, runtime input/result contracts,
diagnostic contracts, execution state data, and abstract external command data.

`story-core` is a Java 17 implementation module. It contains the runtime application use case, pure transition policy,
ports, and bounded diagnostic read model. It has no Minecraft, Forge, logging framework, filesystem, networking,
or GUI dependency.

`story-forge-1.20.1` is the only Forge/Minecraft module. It contains the Forge mod entrypoint, composition root,
logging adapter, clock adapter, and volatile state repository adapter.

## Runtime Model

The runtime is an explicit data transition:

```text
current execution state + neutral runtime input
  -> new execution state + abstract commands + structured diagnostics
```

The initial scaffold advances unknown executions from `root` to `runtime.accepted`, emits `ESE-RUNTIME-001`,
and stores the new state through a port.

## Layers

- Domain: deterministic transition policy.
- Application: state loading, transition coordination, state saving, diagnostic publication.
- Ports: persistence, diagnostics, time.
- Infrastructure: Forge logging/time/state adapters.
- Composition: Forge-only object graph creation.
- Presentation: documented for future diagnostics, not implemented as UI in this scaffold.

## Non-goals

No DSL, parser, editor, networking protocol, production persistence, GUI, concrete game logic,
third-party mod integration, mixins, reflection registration, or plugin discovery is part of this scaffold.
