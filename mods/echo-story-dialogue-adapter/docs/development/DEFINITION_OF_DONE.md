# Definition of Done

D3 считается закрытым только когда выполнено всё ниже:

- adapter production code зависит только от public Story и Dialogue artifacts
- `echo_dialogue:play` является единственным production action type
- payload строго ограничен `dialogueId` и `audienceId`
- deterministic `DialogueSessionId` не меняется между start/recover
- repeated start не создаёт duplicate Dialogue session
- terminal Story completion отправляется только после public terminal Dialogue view
- cancellation идёт через `DialogueService.cancel`
- adapter JAR не содержит Story/Dialogue/EchoScript/API copies
- JVM tests, architecture tests, check, build и build-local orchestration проходят
- GameTest, dedicated-server smoke, isolated client/server и restart acceptance выполнены и задокументированы
- docs отражают фактически выполненные проверки, а не предположения

D3 не включает choices, NPC, portraits, animation, voice, camera, movement locking, quests или generic UI.
