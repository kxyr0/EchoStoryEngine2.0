# Project Map

## `echo-script-api`

- Public API: `EchoScriptParser`, `ParseRequest`, `ParseResult`, `ProcessingLimits`, `LanguageVersion`.
- Source ownership: `SourceId`, `SourcePosition`, `SourceSpan`.
- Diagnostic ownership: `LanguageDiagnostic`, `DiagnosticSeverity`.
- Syntax ownership: immutable records under `api.syntax`.

## `echo-script-language`

- Parser entry point: `EchoScriptLanguage.parser()`.
- Lexer ownership: `language.lexer`.
- Parser ownership: `language.parser`.
- Source text model: `language.source.SourceText`.
- Internal diagnostic collection: `language.diagnostic`.

## Future Adapter Boundary

Adapters may consume `ParseResult` and bind syntax to Story or Dialogue semantics. This repository does not contain those
adapters.

## Forbidden Dependencies

Minecraft, Forge, Fabric, Echo Story Engine, Echo Dialogue, scripting engines, Java compilation, bytecode generation,
reflection execution, dynamic class loading, and runtime plugin discovery are forbidden.
