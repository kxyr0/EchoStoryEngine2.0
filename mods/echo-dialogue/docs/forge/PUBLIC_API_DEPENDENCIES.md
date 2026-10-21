# Public API Dependencies D6

Published local artifacts:

```text
io.github.kxyr0.echodialogue:dialogue-contracts:0.1.0-SNAPSHOT
io.github.kxyr0.echodialogue:dialogue-presentation-api:0.1.0-SNAPSHOT
io.github.kxyr0.echodialogue:dialogue-forge-api-1.20.1:0.1.0-SNAPSHOT
```

Publication task:

```powershell
.\gradlew.bat publishDialogueIntegrationArtifacts --console=plain
```

Output repository:

`D:\JDHSeason\mods\echo-dialogue\build\integration-maven`

External provider mods should compile against those artifacts. They must not depend on:

- `dialogue-core`;
- `dialogue-persistence`;
- `dialogue-echoscript-adapter`;
- `dialogue-forge-1.20.1`;
- Echo NPC;
- Echo Story Engine.

`verifyPresentationProviderIntegrationArtifacts` proves that the separate test-provider JAR does not contain copies of Echo Dialogue public API classes.
