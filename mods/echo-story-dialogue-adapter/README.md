# Echo Story Dialogue Adapter

Echo Story Dialogue Adapter - независимый Forge 1.20.1 мод D3. Он регистрирует public Story extension и
обрабатывает единственное действие `echo_dialogue:play`, стартуя или восстанавливая сессию Echo Dialogue через
публичный `DialogueService`.

Идентичность:

- project name: `Echo Story Dialogue Adapter`
- root project name: `echo-story-dialogue-adapter`
- mod id: `echo_story_dialogue_adapter`
- Maven group: `io.github.kxyr0.echostorydialogueadapter`
- base package: `io.github.kxyr0.echostorydialogueadapter`
- version: `0.1.0-SNAPSHOT`
- Story extension id: `echo_story_dialogue_adapter:bridge`
- canonical action type: `echo_dialogue:play`

Контракт действия:

- обязательные аргументы: `dialogueId`, `audienceId`
- лишние аргументы отклоняются
- `DialogueSessionId` детерминированно строится как `story-action/` + `ActionExecutionId.asString()`
- completion в Story отправляется только после public terminal view Echo Dialogue

Основные документы:

- [D3 execution plan](docs/plans/D3_EXECUTION_PLAN.md)
- [Action contract](docs/integration/ACTION_CONTRACT.md)
- [Testing](docs/development/TESTING.md)
- [Definition of Done](docs/development/DEFINITION_OF_DONE.md)

[История изменений](docs/changelogs/README.md)
