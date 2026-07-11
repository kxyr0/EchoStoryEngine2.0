# Architecture Guardrails

## Automated Checks

The root `check` task runs module checks plus:

- `sourceShapeCheck`: forbidden package/type names, production type size, leaf package size.
- `checkWhitespace`: trailing whitespace check for project-owned text files.

Module checks include:

- JUnit 5 tests;
- ArchUnit architecture tests;
- Checkstyle line/method/complexity/import/brace checks;
- PMD minimal static analysis;
- JaCoCo report generation.

## Architecture Tests

`story-forge-1.20.1/src/test/java/.../ArchitectureRulesTest.java` enforces:

- contracts do not depend on core, Forge, Minecraft, Mojang, or SLF4J;
- core does not depend on Forge, Minecraft, Mojang, or SLF4J;
- core layer access rules;
- application does not depend on platform/composition/infrastructure;
- core does not depend on composition;
- Forge infrastructure adapters implement core ports;
- package slices are cycle-free;
- static fields are final.

## Exception Procedure

An architecture exception is allowed only when all conditions hold:

1. An ADR records reason, alternatives, consequences, removal plan, limited scope, and review date.
2. The exception is narrow and version-controlled.
3. The user sends a separate message with exactly:

```text
ARCHITECTURE_EXCEPTION_APPROVED: <ADR-ID>
```

Do not generate this token as approval. Do not weaken checks, delete tests, lower thresholds, or add broad
suppressions to make a build green.

## Initial Scaffold Policy

No quality exceptions are allowed in the initial scaffold.
