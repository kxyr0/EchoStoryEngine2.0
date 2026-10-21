# Dependency rules

Allowed production dependencies:

- `io.github.kxyr0.echodialogue.api.*`;
- `io.github.kxyr0.echodialogue.presentation.api.*`;
- `io.github.kxyr0.echodialogue.forge.api.*`;
- `io.github.kxyr0.echonpc.api.*`;
- `io.github.kxyr0.echonpc.forge.api.*`;
- Forge/Minecraft server API required for event registration and `MinecraftServer`.

Forbidden production dependencies:

- Echo Story Engine;
- Story-Dialogue Adapter;
- Story-NPC Adapter;
- EchoScript;
- Echo Dialogue implementation packages;
- Echo NPC implementation packages;
- GeckoLib;
- navigation or AI goal systems;
- client renderers;
- remote skin, HTTP, `GameProfile`;
- JSON/Gson and filesystem parsing.

Gradle uses sibling generated Maven repositories. Maven Local is not required.
