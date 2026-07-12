# Dependency Rules

## Gradle Direction

Allowed:

```text
story-contracts <- story-core <- story-definition
story-contracts <- story-core <- story-definition <- story-echoscript-adapter
story-contracts <- story-core <- story-persistence
story-contracts <- story-core <- story-forge-1.20.1
story-contracts <- story-forge-1.20.1
```

`story-forge-1.20.1` may also use `story-contracts` directly. It does not depend on `story-definition` or
`story-persistence` in M3 because Forge resource loading and world-bound persistence are not implemented yet. Reverse
dependencies and cycles are forbidden. `story-echoscript-adapter` is a source frontend and must not be used by
runtime or persistence modules.

## Module Rules

`story-contracts`:

- Java 17 and standard library;
- public API/SPI/data contracts;
- no implementation;
- no Minecraft, Forge, logging framework, serialization framework, networking, filesystem, or GUI.

`story-core`:

- Java 17;
- depends on `story-contracts`;
- compiled story model, domain/application/ports/diagnostics;
- no Forge, Minecraft, filesystem, networking, logging framework, GUI, JSON parser, or concrete game domains.

`story-definition`:

- Java 17;
- depends on `story-core` and `story-contracts`;
- source DTOs, validation, source diagnostics, compiler boundary, and compilation;
- no Gson, EchoScript parser types, Forge, Minecraft, filesystem I/O, networking, logging framework, GUI,
  or concrete game domains.

`story-echoscript-adapter`:

- Java 17;
- depends on `story-definition`, `story-core`, `story-contracts`, and the external EchoScript language artifact;
- owns the EchoScript `.es` StoryResource frontend and all EchoScript type usage;
- must keep EchoScript types out of public Story Engine APIs, persistence DTOs, runtime APIs, and lower modules;
- must not depend on Forge, Minecraft, filesystem I/O, networking, logging framework, GUI, concrete game domains,
  copied EchoScript source, or committed EchoScript jars.

`story-persistence`:

- Java 17;
- depends on `story-core` and `story-contracts`;
- save schema V1, strict checkpoint JSON codec, durable file adapter, recovery planning, persistence diagnostics;
- may depend on Gson only inside the concrete save schema codec boundary;
- must not depend on `story-definition`, Forge, Minecraft, networking, logging framework, GUI, or concrete game
  domains.

`story-forge-1.20.1`:

- only module with ForgeGradle and Forge/Minecraft dependencies;
- entrypoint, lifecycle, composition root, and platform adapters;
- converts platform facts into neutral contracts before calling core.

## Package Rules

- `..story.api..` must not depend on `..story.core..`, `..story.forge..`, Minecraft, Forge, Mojang, or SLF4J.
- `..story.core..` must not depend on Minecraft, Forge, Mojang, or SLF4J.
- `..story.core..` must not depend on `..story.definition..` or JSON parser types.
- `..story.definition..` must not depend on `..story.forge..`, Minecraft, Forge, Mojang, SLF4J, or mixins.
- `..story.definition..` must not depend on EchoScript packages or Gson.
- `..story.echoscript..` may depend on EchoScript packages and must not be imported by contracts, core, definition,
  persistence, or Forge production code in M3.6.
- `..story.definition..` and `..story.persistence..` must not depend on each other.
- `..story.persistence..` must not depend on Forge, Minecraft, Mojang, SLF4J, or mixins.
- Gson usage in `..story.persistence..` is limited to `..story.persistence.schema..`.
- `..story.core.domain..` must not depend on application, composition, presentation, or infrastructure.
- `..story.core.application..` may use domain and ports, but no Forge/composition/infrastructure packages.
- `..story.forge.infrastructure..` implements core ports.
- `..story.forge.composition..` creates objects and is not a business service.

## Automated Enforcement

ArchUnit tests in `story-definition/src/test/java/.../SourcePipelineArchitectureTest.java`,
`story-echoscript-adapter/src/test/java/.../EchoScriptAdapterArchitectureTest.java`,
`story-persistence/src/test/java/.../PersistenceArchitectureTest.java`, and
`story-forge-1.20.1/src/test/java/.../ArchitectureRulesTest.java` enforce these rules.
`sourceShapeCheck` enforces forbidden package/name and package-size rules.
