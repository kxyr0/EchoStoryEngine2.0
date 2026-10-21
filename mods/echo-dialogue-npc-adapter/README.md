# Echo Dialogue NPC Adapter

Echo Dialogue NPC Adapter регистрирует D6 speaker-presentation provider и связывает
`SpeakerId` из Echo Dialogue с `ActorId` из Echo NPC через exact canonical string.

Provider:

- id: `echo_dialogue_npc_adapter:npc_presentation`;
- display name: `Echo NPC Speaker Presentation`;
- mod id: `echo_dialogue_npc_adapter`;
- package: `io.github.kxyr0.echodialoguenpcadapter`.

## Runtime flow

1. Echo Dialogue готовит LINE или CHOICE prompt и отправляет
   `SpeakerPresentationRequest`.
2. Adapter преобразует `request.speakerId().toString()` в `ActorId`.
3. Adapter получает server-scoped `NpcService` через public Forge bridge Echo NPC.
4. Adapter делает только `NpcService.query(actorId)`.
5. При найденном durable actor возвращается dynamic display name и optional
   `SpeakerVisualRef.skinHead(npcSkinId)`.

Если actor не найден или Echo NPC service недоступен, adapter возвращает
`NOT_HANDLED`, и Echo Dialogue показывает authored fallback из `Alias(...)`.

## Сборка

```powershell
.\gradlew.bat test check --console=plain
```

Для isolated build с публикацией sibling public artifacts:

```powershell
.\gradlew.bat n4IntegrationBuild --console=plain
```

Forge GameTest acceptance:

```powershell
$env:GRADLE_OPTS='-Dnet.minecraftforge.gradle.check.certs=false'
.\gradlew.bat runGameTestServer --console=plain
```

Headless acceptance runtime with content:

```powershell
.\gradlew.bat runN4AcceptanceStoryPackServer -PskipSiblingBuild --console=plain
```

Dev-client visual screenshots:

```powershell
.\gradlew.bat runClient -Pn4ClientVisualAcceptance=true -PskipSiblingBuild --console=plain
```

This starts a real Forge userdev client, renders LINE and CHOICE through Echo
Dialogue's client renderer, and writes PNG evidence under
`run-client-visual/n4-client-acceptance/`. It is not a final-JAR client proof.

C1 production content smoke and final-JAR client visual acceptance:

```powershell
.\gradlew.bat runN4ProductionStoryPackSmokeServer -PskipSiblingBuild --console=plain
.\gradlew.bat stageN4ProductionStoryPack -PskipSiblingBuild --console=plain
.\gradlew.bat stageN4ProductionClientVisualAcceptance -PskipSiblingBuild --console=plain
.\gradlew.bat verifyN4ProductionClientVisualAcceptanceOutputs --console=plain
```

The staged client/server mods are written to `build/n4-production-story-pack/mods`.
Use those final JARs in a real Forge 1.20.1 / 47.4.10 launcher profile for
visual client acceptance. ForgeGradle `runClient` is userdev-mapped and is not a
valid final-JAR client proof for this pack.

The production client verifier reads screenshots from
`build/n4-production-client-run/n4-production-client-acceptance/` and checks
`EDNA-PROD-CLIENT-ACCEPTANCE PASS ... display="Отец" visual=jdh:john_father`.
