# Distribution Packaging

Production artifact:

```text
npc-forge-1.20.1/build/libs/echo-npc-0.1.0-SNAPSHOT-all.jar
```

## Содержимое

JAR должен включать:

- `npc-contracts`;
- `npc-core`;
- `npc-persistence`;
- `npc-echoscript-adapter`;
- `npc-forge-api-1.20.1`;
- EchoScript API/language через `jarJar`;
- `META-INF/mods.toml`;
- sample `.npc.es` resources;
- fallback и sample textures.

JAR не должен включать:

- test classes;
- GameTest classes;
- raw sibling source outputs;
- Forge или Minecraft classes;
- remote publishing metadata.

Проверка выполняется задачей:

```powershell
.\gradlew.bat :npc-forge-1.20.1:verifyProductionJarContents --console=plain
```
