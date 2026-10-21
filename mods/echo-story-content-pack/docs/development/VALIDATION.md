# Validation

Aggregate task:

```powershell
.\gradlew.bat validateContent
```

Внутренние проверки:

- `validateContentLayout`;
- `validateContentUtf8`;
- `validateContentAssets`;
- `validateContentReferences`.

Проверяется:

- отсутствие `src/main/java`;
- наличие `META-INF/mods.toml` и `pack.mcmeta`;
- canonical resource roots;
- namespace `jdh`;
- правильные extensions `.es`, `.dialogue.es`, `.npc.es`;
- максимум 7 scripts per leaf folder;
- отсутствие case-insensitive duplicate resource paths;
- lowercase paths без spaces;
- UTF-8;
- LF;
- отсутствие trailing whitespace и tabs в authored text;
- отсутствие common mojibake markers;
- отсутствие hardcoded local machine paths;
- отсутствие source-art formats, nested JARs, `.class`, font files и temp files;
- PNG signature;
- skin/SkinHead 64x64;
- portrait 16..512 px.

`validateContent` не реализует fake EchoScript parser и не дублирует Story/Dialogue/NPC binders. Semantic validation должна идти через `validateWithEchoRuntime` и actual runtime catalogs.

JAR verification:

```powershell
.\gradlew.bat verifyContentJar
```

Эта задача инспектирует фактический archive, а не source tree.

Acceptance JAR verification:

```powershell
.\gradlew.bat verifyAcceptanceContentJar
```

Эта задача проверяет, что test-only acceptance JAR содержит canonical `jdh:content_pack_*` resources, не содержит production slice и не содержит `.class` или nested JAR entries.

Report:

```powershell
.\gradlew.bat contentReport
```

Report создается под `build/reports/content/`.
