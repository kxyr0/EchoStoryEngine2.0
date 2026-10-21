# Testing

## Автоматические проверки

```powershell
.\gradlew.bat projects --console=plain
.\gradlew.bat dependencies --console=plain
.\gradlew.bat test --console=plain
.\gradlew.bat :npc-contracts:test :npc-core:test :npc-persistence:test --console=plain
.\gradlew.bat architectureTest --console=plain
.\gradlew.bat sourceShapeCheck --console=plain
.\gradlew.bat checkWhitespace --console=plain
.\gradlew.bat check --console=plain
.\gradlew.bat build --console=plain
.\gradlew.bat :npc-forge-1.20.1:runGameTestServer --console=plain
```

`check` зависит от unit tests, architecture tests, Checkstyle, PMD,
source-shape, whitespace, forbidden production scan, publication metadata и
production JAR verification.

N2 GameTests keep the same required marker count as N1, but mutating operations
are async: tests expect `QUEUED`, then poll by tick until materialization,
teleport or tombstone removal is visible.

## Scans

Ручные scans перед приемкой:

```powershell
rg -n "echostoryengine|echodialogue|storydialogue|geckolib" npc-*\\src\\main\\java
rg -n "http://|https://sessionserver|gameprofile" npc-*\\src\\main\\java
rg -n "ServiceLoader|Class\\.forName|newInstance" npc-*\\src\\main\\java
rg -n "net\\.minecraft|net\\.minecraftforge" npc-persistence\\src\\main\\java
rg -n "com\\.google\\.gson|java\\.nio\\.file|java\\.io\\." npc-contracts\\src\\main\\java npc-core\\src\\main\\java
```

Ожидаемый результат для запретных production markers - пустой вывод.
