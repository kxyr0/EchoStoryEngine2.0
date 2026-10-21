# Dependency rules

- `animation-contracts` is plain Java 17 and has no Forge, Minecraft or EchoScript dependency.
- `animation-core` is plain Java 17 and owns deterministic math/evaluation.
- `animation-definition` exposes immutable compiled resources and no file I/O.
- `animation-echoscript-adapter` consumes EchoScript syntax and owns animation semantics.
- `animation-forge-api-1.20.1` contains only the public Forge lookup bridge.
- `animation-forge-1.20.1` is the composition root and final production JAR.
- `animation-client-rendering` contains rendering plans, not editor UI.

Leaf production packages are constrained to at most seven files by `sourceShapeCheck`.

