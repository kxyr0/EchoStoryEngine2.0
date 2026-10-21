# D1.1 final report

## 1. Почему D1 требовал отдельной приёмки

D1 реализовал вертикальный поток линейного диалога, но сам факт реализации не доказывает production boundaries: actual client, actual dedicated server, final distributable JAR, invalid reload retention, lifecycle cleanup и side separation должны проверяться отдельно от unit tests и Gradle source-set runtime.

## 2. Краткий итог D1.1

Кодовая часть D1.1 hardening выполнена: source/line/column diagnostics, client stale packet guards, disconnect clear, idempotent host shutdown, semantic invalid-reload coverage и automated final-JAR verification добавлены.

Честный итоговый статус: `acceptance pending`. Причины:

- normal `runServer` после ручного принятия EULA дошёл до `Done`, загрузил 1 диалог и поднял Dialogue host;
- isolated final-JAR client rerun загрузил final JAR, но multiplayer connect не дошёл до `echo_dialogue:main` acceptance;
- resize/disconnect visual checklist после D1.1 не был заново наблюдён.

## 3. Обнаруженные дефекты

- Syntax diagnostics теряли logical source id и line/column при отображении Echo Dialogue diagnostics.
- Semantic `Line(...)` diagnostics для unresolved alias не имели source subject.
- `/echo_dialogue diagnostics` не показывал subject.
- Client state мог принять stale same-session open/update и преждевременно снять wait lock.
- Client state не очищался явно на client logout/disconnect.
- Host shutdown был неявным: composition root просто обнулял host.
- Не было automated `verifyProductionJarContents`.
- Документация противоречила фактическому D1 status.

## 4. Исправленные дефекты

- `DiagnosticSubject` расширен optional `line`/`column`.
- `EchoScriptDiagnosticAdapter` переносит `SourceId` и `SourceSpan`.
- `DialogueSemanticDiagnostics` умеет строить source-bound semantic diagnostics.
- `NodeLineReader` привязывает invalid `Line(...)` diagnostics к call-site.
- `ForgeDiagnosticFormatter` и `ForgeDiagnosticSink` выводят source subject без raw source dump.
- `ClientDialogueState` игнорирует stale update/close и чужой open при active overlay.
- `ClientDialogueClient` очищает state на `ClientPlayerNetworkEvent.LoggingOut`.
- `ForgeDialogueHost.shutdown()` стал idempotent.
- `ForgeDialogueCompositionRoot` вызывает shutdown на repeated start/stop lifecycle.
- `verifyProductionJarContents` добавлен в Forge Gradle module и подключён к `check`.

## 5. Итоговая структура модулей

```text
dialogue-contracts
  <- dialogue-core
      <- dialogue-echoscript-adapter
          <- dialogue-forge-1.20.1
```

Echo Story Engine не подключён.

## 6. Фактический source/reload pipeline

`data/<namespace>/echo_dialogue/dialogues/**/*.dialogue.es` читается как UTF-8 -> EchoScript parser -> DialogueResource binder -> `DialogueCatalogCandidate` -> `DialogueCatalogState.install()` -> valid candidate increments revision, invalid candidate keeps previous snapshot.

## 7. Invalid reload test

- Исходный каталог: valid `sample:greeting`, revision `1`.
- Ошибка: `Line("ghost", "Где ты был?");`.
- Диагностика: `EDLG-SEM-011`, category `SEMANTIC_VALIDATION`, source subject present.
- Доказательство retention: state revision не изменился, `sample:greeting` остался видимым.
- Reload после исправления: fixed candidate installed, revision увеличился на `1`.

Покрыто unit/integration test и GameTest `ForgeDialogueGameTests.invalidCandidateRetainsCatalog`.

## 8. GameTest

- Exact task: `.\gradlew.bat :dialogue-forge-1.20.1:runGameTestServer --console=plain`.
- Discovered: `4`.
- Executed: `4`.
- Passed: `4`.
- Failed: `0`.
- Exit code: `0`.
- Names:
  - `ForgeDialogueGameTests.echoDialogueCommandsExist`
  - `ForgeDialogueGameTests.dialogueResourceCompilesInForgeRuntime`
  - `ForgeDialogueGameTests.controlledAudienceSessionLifecycle`
  - `ForgeDialogueGameTests.invalidCandidateRetainsCatalog`

## 9. Normal dedicated-server smoke

- Command: `.\gradlew.bat :dialogue-forge-1.20.1:runServer --console=plain`.
- EULA status: `dialogue-forge-1.20.1/run/eula.txt` вручную изменён пользователем на `eula=true`.
- Result: server reached `Done (7.479s)! For help, type "help"`.
- Log evidence: `EDLG-FORGE-003 Reload диалогов принят: 1`, `EDLG-FORGE-006 Dialogue host активен`.
- Error scan: no `Exception`, `ERROR`, `FATAL`, `NoClassDefFoundError` or `ClassNotFoundException` in selected latest-log markers.
- Stop note: console `stop` input did not reach the process through the automation terminal; this run was cleaned up with Ctrl+C/batch termination. No Java processes remained afterward.
- Acceptance status: dedicated-server load smoke passed; graceful `stop` for this specific PTY run is not claimed.

## 10. Real client acceptance

Previous D1 user-observed smoke remains valid evidence for basic overlay: centered panel, speaker `Отец`, Russian text and final close were confirmed by user.

D1.1 rerun did not manually verify resize, pause, disconnect, vanilla chat isolation or isolated-client overlay progression. These remain user-pending.

## 11. Final production JAR

- Path: `D:\JDHSeason\mods\echo-dialogue\dialogue-forge-1.20.1\build\libs\echo-dialogue-0.1.0-SNAPSHOT.jar`.
- Size: `196170` bytes.
- Entries: `164`.
- Contains `META-INF/mods.toml`, sample dialogue, Dialogue production classes, Forge platform classes, client overlay/network classes and EchoScript API/language.
- `HasModuleInfo=0`.
- Forbidden-content scan found no JUnit, GameTest, Minecraft, Forge, Story Engine, docs, run/build metadata or `META-INF/maven`.

## 12. Isolated final-JAR server result

- Directory: `dialogue-forge-1.20.1/build/smoke/server`.
- `mods` contained only current `echo-dialogue-0.1.0-SNAPSHOT.jar`.
- Server reached `Done`.
- Log evidence: `EDLG-FORGE-003 Reload диалогов принят: 1`, `EDLG-FORGE-006 Dialogue host активен`, `EDLG-FORGE-007 Dialogue host остановлен; сессии очищены`.
- Stop command succeeded; process exit code `0`.

## 13. Isolated final-JAR client result

- Directory: `dialogue-forge-1.20.1/build/smoke/client`.
- `mods` contained only current `echo-dialogue-0.1.0-SNAPSHOT.jar`.
- Client loaded final JAR and Echo Dialogue resources; no missing EchoScript classes were found.
- Multiplayer quickPlay to `127.0.0.1` did not complete: server logged `EchoSmoke ... lost connection: Disconnected` before channel acceptance.
- Status: load partial, connect pending.

## 14. Lifecycle and memory-leak hardening

- Host has no global `MinecraftServer`.
- `shutdown()` clears session holder and is idempotent.
- Player logout cancels active session.
- Client logout clears overlay state.
- Unit test verifies stopped diagnostic emitted once.

## 15. Networking/input hardening

- Packet payload remains `sessionId` plus expected `revision`.
- Sender identity comes from Forge context.
- Client wait lock blocks duplicate advance.
- Same-session stale open/update is ignored.
- Stale close with older revision is ignored.
- Different session open cannot overwrite active overlay.

## 16. Created or changed production types

- `DiagnosticSubject`: diagnostic target plus optional source coordinates.
- `ActiveDialogueSessions`: active-session holder plus explicit clear.
- `EchoScriptDiagnosticAdapter`: maps EchoScript diagnostics to Echo Dialogue diagnostics.
- `DialogueSemanticDiagnostics`: semantic diagnostic factory.
- `NodeLineReader`: reads and validates ordered `Line(...)` calls.
- `ForgeDialogueCompositionRoot`: Forge lifecycle wiring.
- `ForgeDiagnosticFormatter`: command-facing diagnostic formatting.
- `ForgeDiagnosticSink`: standard Forge log sink.
- `ForgeDialogueHost`: server-side session host.
- `ClientDialogueState`: client presentation state machine.
- `ClientDialogueClient`: client overlay/input/event bridge.

## 17. One exact responsibility for every changed production type

Each changed type keeps one responsibility matching section 16. No new D2 feature responsibility was added.

## 18. Production type count by package

No leaf production package exceeds seven files. Current high-water marks: `core/session` has `7`, adapter `binding` has `6`, contracts `identity` has `6`, Forge `resource` has `5`.

## 19. Seven-file limit result

`.\gradlew.bat sourceShapeCheck --console=plain` passed.

## 20. Executed commands and results

- `.\gradlew.bat projects --console=plain`: success.
- `.\gradlew.bat test --console=plain`: success.
- Focused module tests for all four modules: success.
- `.\gradlew.bat architectureTest sourceShapeCheck checkWhitespace --console=plain`: success.
- `.\gradlew.bat :dialogue-forge-1.20.1:verifyProductionJarContents --console=plain`: success.
- `.\gradlew.bat check build --console=plain`: success.
- `.\gradlew.bat :dialogue-forge-1.20.1:runGameTestServer --console=plain`: success, `4/4`.
- `.\gradlew.bat :dialogue-forge-1.20.1:runServer --console=plain`: after manual EULA, dedicated-server load smoke passed; PTY `stop` input did not complete, process cleaned up with Ctrl+C.
- Isolated final-JAR server smoke: success, exit code `0`.
- Isolated final-JAR client load/connect rerun: load partial, connect pending.

JVM tests: `11` XML files, `34` tests, `0` failures, `0` errors, `0` skipped.

## 21. Checks not executed and exact reasons

- Normal dedicated server graceful `stop` confirmation: not claimed for the PTY rerun because console input did not reach the Minecraft server process.
- Isolated client visual overlay progression: not executed by direct observation during D1.1 rerun.
- Isolated client multiplayer channel acceptance: not reached; connection ended before `echo_dialogue:main` negotiation.

## 22. Remaining risks

- Isolated client script under Java 21 is fragile and did not complete multiplayer quickPlay.
- Visual resize/disconnect/chat-history checklist needs a human or reliable UI automation pass.

## 23. Deferred functionality

Persistence, choices, branching, NPC lookup, portraits, animation, voice, sound, typewriter, camera, movement lock, generic UI, editor and Story integration remain deferred.

## 24. Honest final status

`acceptance pending`.

Automated code/build/GameTest/artifact, normal dedicated-server load smoke and isolated server acceptance passed. Isolated client connect and D1.1 visual checklist remain pending.

## 25. Forbidden feature confirmation

Story integration, persistence, choices, portraits and NPC features were not implemented.

## 26. Next milestone

Only after complete D1.1 acceptance:

`D2 — Durable Dialogue Session Persistence & Recovery`.
