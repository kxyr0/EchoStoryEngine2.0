# Building

Content pack собирается без production Java:

```powershell
.\gradlew.bat build
```

Итоговый JAR:

`build/libs/echo-story-content-pack-0.1.0-SNAPSHOT.jar`

Test-only acceptance JAR:

```powershell
.\gradlew.bat acceptanceContentJar verifyAcceptanceContentJar
```

Итоговый acceptance JAR:

`build/libs/echo-story-content-pack-0.1.0-SNAPSHOT-acceptance.jar`

Он использует тот же `modId`, но содержит `src/acceptance/resources` вместо production content. Не устанавливайте его вместе с production content JAR.

Archive settings:

- reproducible file order;
- timestamps не сохраняются;
- docs/templates/acceptance/source-art не входят в JAR.

`mods.toml` использует:

```toml
modLoader="lowcodefml"
```

Это устраняет необходимость в `@Mod` class.
