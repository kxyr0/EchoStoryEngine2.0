# Architecture

EchoScript is a standalone Java 17 library. Its only runtime pipeline is:

`source text -> source model -> lexer -> token stream -> parser -> immutable syntax tree -> diagnostics`

The library never executes `.es` source. It does not resolve consumer-domain symbols and does not know what
`DialogueResource`, `StoryResourceDef`, `NodeDef`, `Line`, `Set`, or `Action` mean.

## Modules

- `echo-script-api`: public contracts and immutable syntax records.
- `echo-script-language`: lexer, parser, internal diagnostics, and parser recovery.

## Runtime Dependencies

- `echo-script-api`: no project dependencies.
- `echo-script-language`: depends on `echo-script-api` only.

Verification dependencies are scoped to tests and quality gates.
