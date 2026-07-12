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
diagnostic contracts, execution phase/state data, external action completion categories, and abstract external
command data.

`story-core` is a Java 17 implementation module. It contains the runtime application use case, minimal programmatic
story definition model, pure transition policy, ports, and bounded diagnostic read model. It has no Minecraft, Forge,
logging framework, filesystem, networking, or GUI dependency.

`story-forge-1.20.1` is the only Forge/Minecraft module. It contains the Forge mod entrypoint, composition root,
logging adapter, clock adapter, and volatile state repository adapter.

## Runtime Model

The runtime is an explicit data transition:

```text
current execution state + neutral runtime input
  -> new execution state + abstract commands + structured diagnostics
```

M1 implements one deterministic hello-story vertical slice:

```text
Ready state + start input
  -> running start node
  -> story-owned variable write
  -> condition check
  -> abstract external command
  -> Waiting state

Waiting state + matching external action completion
  -> running next node
  -> Finished state
```

External actions use namespaced action types and stable action execution ids. Completion input carries explicit
success, failure, cancellation, or timeout categories. Duplicate completions are ignored with a structured diagnostic;
unknown completion ids are handled without changing the execution. Unsupported action types fail only the affected
execution with a structured diagnostic.

Synchronous node evaluation is bounded by a strict transition budget. Exceeding the budget fails the affected
execution instead of looping on the server context.

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
