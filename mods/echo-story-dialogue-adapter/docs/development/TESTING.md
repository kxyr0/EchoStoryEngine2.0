# Testing

Основные команды adapter repository:

```powershell
.\gradlew.bat projects --console=plain
.\gradlew.bat dependencies --console=plain
.\gradlew.bat test --console=plain
.\gradlew.bat architectureTest --console=plain
.\gradlew.bat sourceShapeCheck --console=plain
.\gradlew.bat checkWhitespace --console=plain
.\gradlew.bat check --console=plain
.\gradlew.bat build --console=plain
.\gradlew.bat runGameTestServer --console=plain
.\gradlew.bat buildAdapterAgainstPublishedArtifacts --console=plain
```

`runGameTestServer` запускает adapter GameTest в mapped Forge userdev. Story и Dialogue подключаются только как
`gameTestRuntimeOnly fg.deobf(...)` из sibling final JARs через `flatDir`; production reobf JAR не копируются в
`run/mods`.

Текущий adapter GameTest содержит 3 обязательных сценария: регистрацию модов/команд, public Dialogue terminal
cancellation -> Story completion и terminal recovery после закрытия исходного observer-а -> Story completion.

Ограничение: `git diff --check` возможен только после `git init` или переноса проекта в существующий git repository.

Runtime acceptance команды должны запускаться отдельно и не считаются пройденными, пока нет точного вывода.
