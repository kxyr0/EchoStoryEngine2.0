# EchoScript

EchoScript is a standalone plain Java 17 language frontend for `.es` source files.

The project currently provides a safe syntax-only pipeline:

`UTF-8 source -> immutable source model -> lexer -> token stream -> parser -> immutable syntax tree -> diagnostics`

EchoScript does not execute source code and does not understand Story, Dialogue, Minecraft, Forge, Fabric, or game
domain semantics. Consumer projects can adapt the syntax tree later, but this repository owns only language syntax.

## Modules

- `echo-script-api`: public parser-facing contracts, source identity, diagnostics, limits, and syntax tree records.
- `echo-script-language`: hand-written lexer and recursive-descent parser implementation.

## Build

Use the Gradle Wrapper from the repository root:

```powershell
.\gradlew.bat check
```

## Maven Local

Publish local snapshots with:

```powershell
.\gradlew.bat publishToMavenLocal
```

Published coordinates:

- `io.github.kxyr0.echoscript:echo-script-api:0.1.0-SNAPSHOT`
- `io.github.kxyr0.echoscript:echo-script-language:0.1.0-SNAPSHOT`

No license has been selected yet. A `LICENSE` file is intentionally absent until the project owner chooses one.
