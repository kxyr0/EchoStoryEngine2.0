# Правила зависимостей

Production code может зависеть только от публичных API:

- `io.github.kxyr0.echostoryengine:story-contracts:0.1.0`
- `io.github.kxyr0.echostoryengine:story-extension-api:0.1.0`
- `io.github.kxyr0.echostoryengine:story-extension-forge-1.20.1:0.1.0`
- `io.github.kxyr0.echodialogue:dialogue-contracts:0.1.0-SNAPSHOT`
- `io.github.kxyr0.echodialogue:dialogue-forge-api-1.20.1:0.1.0-SNAPSHOT`

Запрещены зависимости на:

- Story core, persistence, definition и Forge internals
- Dialogue core, persistence, echoscript adapter, Forge host/client/network internals
- EchoScript
- project dependencies на sibling repositories
- `ServiceLoader`, reflection, classpath scanning и arbitrary executors

`build.gradle` использует `compileOnly` для runtime API artifacts, чтобы adapter JAR не бандлил Story/Dialogue classes.
