# ADR-0010: Remove Direct Echo NPC Dependency

## Decision

Echo Animation must not compile against the Echo NPC runtime JAR. The
file-based `compileOnly` dependency on `../echo-npc/...-all.jar` is removed.

## Consequences

- Echo Animation builds without Echo NPC present.
- Echo Animation contains no `EchoNpcEntity`, `EchoNpcEntityRenderer` or
  NPC-specific layer classes.
- NPC face rendering integration moves to a separate adapter.
