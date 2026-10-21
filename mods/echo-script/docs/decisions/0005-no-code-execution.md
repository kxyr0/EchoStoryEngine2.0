# 0005 No Arbitrary Code Execution

## Status

Accepted.

## Context

EchoScript source must be data, not executable JVM behavior.

## Decision

The frontend never uses scripting engines, Java compilation, bytecode generation, reflection invocation, dynamic class
loading, MethodHandle execution, or plugin discovery.

## Alternatives

- Java scripting.
- Generated Java or bytecode.
- Reflection-based dispatch.

## Consequences

Source cannot access files, network, processes, threads, environment variables, Java classes, or game APIs.

## Enforcement

Architecture tests and source scans reject forbidden technologies.

## Related Documents

- `docs/language/SAFETY.md`
- `docs/architecture/ARCHITECTURE_GUARDRAILS.md`
