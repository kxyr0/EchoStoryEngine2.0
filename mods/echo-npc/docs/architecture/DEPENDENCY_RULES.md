# Dependency Rules

## Разрешенный граф

```text
npc-contracts
  <- npc-core
  <- npc-persistence
  <- npc-forge-1.20.1

npc-contracts
  <- npc-core
  <- npc-echoscript-adapter
  <- npc-forge-1.20.1

npc-contracts
  <- npc-forge-api-1.20.1
  <- npc-forge-1.20.1
```

## Запреты production code

- Нет зависимостей на Echo Story Engine.
- Нет зависимостей на Echo Dialogue.
- Нет зависимостей на Story-Dialogue Adapter.
- Нет dependencies на Echo Animation, GeckoLib или navigation mods.
- Нет mixins, ServiceLoader, classpath scanning, DI frameworks и scripting engines.
- Нет Minecraft или Forge imports в `npc-contracts`, `npc-core`,
  `npc-persistence`, `npc-echoscript-adapter`.
- Нет EchoScript syntax types в `npc-contracts` и `npc-core`.
- Нет Path/File/Gson/JSON types в `npc-contracts` и public repository port.
- Нет EchoScript AST, Forge/Minecraft classes или renderer/client classes в
  `npc-persistence`.

## EchoScript

Разрешены только публичные artifacts:

- `io.github.kxyr0.echoscript:echo-script-api:0.5.0-SNAPSHOT`
- `io.github.kxyr0.echoscript:echo-script-language:0.5.0-SNAPSHOT`

Они используются для parse и AST inspection. Echo NPC не выполняет скрипты,
не компилирует Java и не вызывает reflection-based user code.

## Automated acceptance

Обычная сборка резолвит EchoScript из `build/external-maven`, который создается
задачей `prepareEchoScriptArtifacts`. Maven Local поддержан только для удобства:
`-PenableMavenLocal=true`.
