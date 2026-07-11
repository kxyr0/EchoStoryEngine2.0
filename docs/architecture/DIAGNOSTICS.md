# Diagnostics

## Policy

Core does not depend on a logging framework. It emits `StoryDiagnostic` values through `DiagnosticSink`.
Platform adapters decide how to log or display diagnostics.

Echo Story Engine must not create subsystem log files such as `story.log`, `runtime.log`, `validation.log`,
`persistence.log`, `network.log`, or adapter-specific logs. Physical logs created by Forge, Gradle, or the launcher
are external build/runtime artifacts and must not be committed.

## Current Implementation

- Diagnostic code: `ESE-RUNTIME-001`
- Severity: `INFO`
- Subsystem: `runtime`
- Sink port: `DiagnosticSink`
- Forge adapter: `ForgeDiagnosticSink`
- Read model: `InMemoryDiagnosticBuffer`

The Forge adapter uses one logical logger namespace: `io.github.kxyr0.echostoryengine.story`.

## Diagnostic Shape

Each diagnostic has:

- stable code;
- severity;
- subsystem;
- human-readable message;
- subject context for story, execution, node, external action, correlation, and source location;
- bounded key/value context;
- cause category;
- timestamp supplied by a port;
- optional operator hint represented as an empty string when absent.

## Severity Policy

- `TRACE`: detailed state-machine transitions only under explicit debug mode.
- `DEBUG`: developer context, not normal user events.
- `INFO`: startup summary and rare lifecycle events.
- `WARN`: recoverable deviation or fallback.
- `ERROR`: lost execution, broken external contract, corrupted data, or unhandled boundary failure.

## Anti-Spam Rules

Future adapters must deduplicate repeated diagnostics, rate-limit repeats, summarize suppressed repeats, avoid
log-and-rethrow, bound context values, and avoid dumping full story state or sensitive values.

## Diagnostic Read Model

Text commands and visual diagnostics must read from the same structured model. The current bounded buffer is in-memory
only and is not a second log stream.
