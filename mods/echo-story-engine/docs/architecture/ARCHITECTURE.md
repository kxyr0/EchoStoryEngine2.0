# Architecture

## Style

Echo Story Engine combines Clean Architecture, Ports and Adapters, Functional Core / Imperative Shell,
Dependency Inversion, explicit serializable state machines, and server-authoritative execution.

These names are not folder names by themselves. A package exists only when it has a current, testable purpose.

## Modules

```text
story-contracts <- story-core <- story-definition
story-contracts <- story-core <- story-definition <- story-echoscript-adapter
story-contracts <- story-core <- story-persistence
story-contracts <- story-core <- story-definition <- story-echoscript-adapter <- story-forge-1.20.1
story-contracts <- story-core <- story-persistence <- story-forge-1.20.1
story-contracts <- story-core <- story-forge-1.20.1
story-contracts <- story-forge-1.20.1
```

`story-contracts` is a Java 17 public contract module. It exposes identifiers, runtime input/result contracts,
diagnostic contracts, execution phase/state data, external action completion categories, and abstract external
command data. `CompiledStoryFingerprint` is the neutral compatibility token used by durable checkpoints.

`story-core` is a Java 17 implementation module. It contains the runtime application use case, immutable compiled
story definition model, pure transition policy, ports, and bounded diagnostic read model. It has no Minecraft, Forge,
logging framework, filesystem, networking, JSON, or GUI dependency.

`story-definition` is a Java 17 source-processing module. It contains source input/data records, structural
validation, semantic graph validation, source diagnostics, a source compiler interface, and compilation into
`StoryDefinition`. It has no concrete frontend parser dependency.

`story-echoscript-adapter` is a Java 17 source frontend module. It consumes the published EchoScript language artifact,
parses `.es` source, binds the supported `StoryResource` profile into neutral source DTOs, adapts diagnostics, and
then delegates validation and compilation to `story-definition`.

`story-persistence` is a Java 17 persistence module. It owns save schema V1, strict JSON checkpoint encoding,
portable file persistence, backup/temp handling, optimistic revision checks, and recovery planning. Gson is scoped to
the schema codec and does not appear in core ports or runtime APIs.

`story-forge-1.20.1` is the only Forge/Minecraft module. It contains the Forge mod entrypoint, composition root,
server lifecycle coordinator, `.es` server resource reload listener, immutable catalog holder, world-bound persistence
queue, diagnostic command tree, logging/time adapters, and production packaging checks.

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

External actions use namespaced action types and per-execution action execution ids. Completion input carries explicit
success, failure, cancellation, or timeout categories. Duplicate completions are ignored with a structured diagnostic;
unknown completion ids are handled without changing the execution. Unsupported action types fail only the affected
execution with a structured diagnostic.

Synchronous node evaluation is bounded by a strict transition budget. Exceeding the budget fails the affected
execution instead of looping on the server context.

M3 adds durable save-before-dispatch ordering:

```text
runtime input -> pure transition -> durable checkpoint save -> expose abstract commands
```

If save fails or an optimistic revision conflict occurs, commands are suppressed and the previous durable state remains
authoritative. WAITING recovery recreates one abstract command with the same `ActionExecutionId`; adapters must treat
that id as the idempotency key.

M4 wires this ordering into Forge:

```text
server resource reload -> immutable catalog
server request -> persistence queue load -> transition -> persistence queue save -> server-thread dispatch
```

The persistence worker performs file I/O only. It returns completion callbacks drained on the server tick. Production
external action execution is still outside M4; missing handlers are diagnosed without adding gameplay semantics.

## Source Pipeline

M3.6 implements this source pipeline:

```text
StorySourceInput
  -> EchoScript V1 parser
  -> StoryResource profile binder
  -> StructuralStoryValidator
  -> SemanticStoryValidator
  -> SourceStoryCompiler
  -> StoryDefinition
  -> RuntimeTransitionPolicy
```

The EchoScript adapter converts allowlisted `StoryResource` declarations into immutable neutral source DTOs and reports
syntax, language, and binding diagnostics. Structural validation checks required fields, supported source format
version, identifiers, node kind shape, duplicate nodes, required node fields, and invalid node field combinations.
Semantic validation checks start-node existence, transition targets, reachable finish, unreachable nodes, synchronous
cycles, and action type identifier syntax.

Compilation runs only after structural and semantic validation have no `ERROR` diagnostics. It produces the existing
core `StoryDefinition` and does not retain EchoScript AST objects, parser DTOs, source collections, or runtime
execution state.

## Layers

- Source processing: EchoScript frontend adapter, source DTOs, validators, compiler.
- Domain: compiled story model and deterministic transition policy.
- Application: state loading, transition coordination, state saving, diagnostic publication.
- Ports: persistence, diagnostics, time.
- Persistence adapters: save schema, codec, durable file repository, recovery planner.
- Infrastructure: Forge logging/time, resource, persistence, diagnostics, and host adapters.
- Composition: Forge-only object graph creation.
- Presentation: read-only Forge diagnostic commands, not concrete UI.

## Non-goals

No editor, networking protocol, GUI, concrete game logic, third-party mod integration, production external action
registry, mixins, reflection registration, or plugin discovery is part of this scaffold.
