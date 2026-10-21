# Dependency Rules

Runtime dependency direction is one-way:

```text
echo-script-language -> echo-script-api
```

Rules:

- `echo-script-api` must not depend on any project module.
- `echo-script-language` must not depend on Story Engine, Echo Dialogue, Minecraft, Forge, Fabric, or loaders.
- No cyclic module dependency is allowed.
- Parser internals stay in `echo-script-language`.
- Consumer adapters are future external projects and are not implemented here.

The `architectureTest` task verifies the module build files, forbidden imports, forbidden technology strings, and package
cycles.
