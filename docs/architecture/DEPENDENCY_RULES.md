# Dependency Rules

## Gradle Direction

Allowed:

```text
story-contracts <- story-core <- story-forge-1.20.1
```

`story-forge-1.20.1` may also use `story-contracts` directly. Reverse dependencies and cycles are forbidden.

## Module Rules

`story-contracts`:

- Java 17 and standard library;
- public API/SPI/data contracts;
- no implementation;
- no Minecraft, Forge, logging framework, serialization framework, networking, filesystem, or GUI.

`story-core`:

- Java 17;
- depends on `story-contracts`;
- domain/application/ports/diagnostics;
- no Forge, Minecraft, filesystem, networking, logging framework, GUI, or concrete game domains.

`story-forge-1.20.1`:

- only module with ForgeGradle and Forge/Minecraft dependencies;
- entrypoint, lifecycle, composition root, and platform adapters;
- converts platform facts into neutral contracts before calling core.

## Package Rules

- `..story.api..` must not depend on `..story.core..`, `..story.forge..`, Minecraft, Forge, Mojang, or SLF4J.
- `..story.core..` must not depend on Minecraft, Forge, Mojang, or SLF4J.
- `..story.core.domain..` must not depend on application, composition, presentation, or infrastructure.
- `..story.core.application..` may use domain and ports, but no Forge/composition/infrastructure packages.
- `..story.forge.infrastructure..` implements core ports.
- `..story.forge.composition..` creates objects and is not a business service.

## Automated Enforcement

ArchUnit tests in `story-forge-1.20.1/src/test/java/.../ArchitectureRulesTest.java` enforce these rules.
`sourceShapeCheck` enforces forbidden package/name and package-size rules.
