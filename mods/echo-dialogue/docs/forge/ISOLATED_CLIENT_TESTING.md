# Isolated client testing D5

## Цель

Проверить, что clean Forge 1.20.1 client загружает только final Echo Dialogue production JAR и не требует отдельных module/EchoScript jars.

## Runtime layout

Используемый isolated layout:

```text
D:\JDHSeason\mods\echo-dialogue\dialogue-forge-1.20.1\build\smoke\client
```

`mods` должен содержать только:

```text
echo-dialogue-0.1.0-SNAPSHOT-all.jar
```

Не добавлять:

- `dialogue-core.jar`;
- `dialogue-persistence.jar`;
- `dialogue-contracts.jar`;
- `dialogue-echoscript-adapter.jar`;
- `echo-script-api.jar`;
- `echo-script-language.jar`;
- `echo-ui-animations.jar`;
- Echo Story Engine;
- source-set outputs;
- test classes.

## Quick launch

Для быстрой ручной visual acceptance использовать dev client. Это самый короткий
путь: отдельная Gradle-задача сначала кладёт внешний D6 provider JAR в
`dialogue-forge-1.20.1\run\mods`, затем `runClient` поднимает обычный Forge
client с тем же game directory:

```powershell
cd D:\JDHSeason\mods\echo-dialogue
.\gradlew.bat :dialogue-forge-1.20.1:preparePresentationProviderClientAcceptanceMod --console=plain
.\gradlew.bat :dialogue-forge-1.20.1:runClient --console=plain
```

Перед production isolated visual acceptance убедиться, что server/client profile
используют только актуальные JARs из:

```text
D:\JDHSeason\mods\echo-dialogue\build\presentation-provider-smoke\mods
```

Для Echo Dialogue-only client smoke копировать только:

```text
echo-dialogue-0.1.0-SNAPSHOT-all.jar
```

Для D6 provider client smoke копировать:

```text
echo-dialogue-0.1.0-SNAPSHOT-all.jar
echo-dialogue-presentation-test-0.1.0-SNAPSHOT.jar
```

Isolated server:

```powershell
cd D:\JDHSeason\mods\echo-dialogue\dialogue-forge-1.20.1\build\smoke\server
.\run.bat --nogui
```

There is no tracked `run-production-client.ps1` helper in this repository.
Production isolated client acceptance must use a real Forge 1.20.1-47.4.10
client profile or launcher instance with the staged JARs above. оператор must mark
that part pending unless it was actually observed.

## Automated log checks

Проверить:

- client нашёл `echo_dialogue`;
- no missing embedded classes;
- no `ClassNotFoundException`;
- no `NoClassDefFoundError`;
- channel `echo_dialogue:main` accepted protocol `5`;
- client reaches a test world or connects to isolated server;
- server log records joined player when multiplayer smoke is used.

## Manual visual checklist

1. Запустить isolated Forge client.
2. Создать test world с commands enabled или подключиться к isolated server.
3. Выполнить `/reload`.
4. Выполнить `/echo_dialogue health`.
5. Выполнить `/echo_dialogue dialogues`.
6. Выполнить `/echo_dialogue play sample:greeting`.
7. Проверить centered compact overlay.
8. Проверить строку в формате `[Отец]: Где ты был?`.
9. Проверить русский line text без mojibake.
10. Проверить, что подсказок вида `ПКМ`, `Enter`, `ЛКМ`, `Space` или `Пробел` на overlay нет.
11. Проверить typewriter reveal и fade/scale in.
12. Нажать Enter один раз: должен быть ровно один advance.
13. Удержать Enter: не должно быть uncontrolled skip.
14. Нажать ПКМ: должен быть ровно один advance.
15. Дойти до final line: overlay закрывается через fade/scale out.
16. Проверить vanilla chat history: dialogue text не добавлен.
17. Открыть pause menu и закрыть: session state не повреждён.
18. Изменить размер окна: текст не выходит за screen/panel.
19. Disconnect/return to title: overlay очищен.
20. Открыть другой world: старый overlay не появляется.
21. После disconnect/reconnect к world с durable ACTIVE session текущая строка восстанавливается без увеличения revision.
22. После completion terminal record остаётся видимым через diagnostics/read model до cleanup/ack.
23. Выполнить `/echo_dialogue play sample:manual_choice_1`.
24. Проверить, что после вопроса panel плавно расширяется под choices: `1. Текст выбора` ... `4. Текст выбора`.
25. Проверить клавиши `1`, `2`, `3`, `4`, ЛКМ и ПКМ по варианту.
26. Проверить, что на CHOICE ходьба и поворот камеры заблокированы, а курсор мыши свободен.
27. Выполнить `/echo_dialogue play sample:long_overflow`.
28. Проверить, что длинная реплика переносится внутри `MAX_TEXT_WIDTH`, показывается полностью по высоте и не перекрывает прицел ближе `CROSSHAIR_PADDING_Y`.
29. Выполнить `/echo_dialogue play sample:visual_showcase`.
30. Проверить skin-head у `[Отец]`: face orientation правильный, outer head layer виден, body pixels не попадают в голову.
31. Нажать Enter и проверить portrait у `[Торговец]`: alpha и aspect ratio корректны.
32. Проверить, что текст `[Торговец]: ...` центрирован по Y относительно portrait/head row.
33. Дойти до choices и проверить, что portrait остаётся у prompt speaker, а `1`/`2`, ЛКМ и ПКМ выбирают только visible choice.
34. Проверить missing-resource fallback через datapack/resource pack: текст остаётся readable, session не закрывается.
35. Выполнить client resource reload: visual cache очищается, DialogueSessionId/revision не меняются.

Если оператор не наблюдает эти пункты напрямую, они остаются `user-pending` или `user-verified`, но не auto-passed.

## Acceptance result template

После ручного прогона зафиксировать результат в таком виде:

```text
D5 isolated client visual acceptance:
- runtime: dev client | isolated client
- date:
- sample:greeting text-only regression: pass | fail
- sample:manual_choice_1 choices regression: pass | fail
- sample:long_overflow fixed-width/height regression: pass | fail
- sample:visual_showcase SkinHead("sample:father"): pass | fail
- sample:visual_showcase Portrait("sample:merchant"): pass | fail
- visual text Y-centering: pass | fail
- visual choice prompt and hitboxes: pass | fail
- CHOICE input lock and free mouse cursor: pass | fail
- missing-resource text-only fallback: pass | fail
- F3+T resource reload cache reset: pass | fail
- LINE restart/reconnect visual recovery: pass | fail
- CHOICE restart/reconnect visual recovery: pass | fail
- no stale visual after return to title/second world: pass | fail
- notes:
```

D5 можно закрывать только когда все `fail` разобраны и все visual-only пункты
имеют `pass` или явно согласованный `deferred` с причиной.

## D6 dynamic provider acceptance

Сначала подготовить staged mods:

```powershell
.\gradlew.bat verifyPresentationProviderIntegrationArtifacts --console=plain
```

Скопировать в isolated server/client только JARs из:

`D:\JDHSeason\mods\echo-dialogue\build\presentation-provider-smoke\mods`

Проверки в клиенте:

- `/echo_dialogue providers` показывает `echo_dialogue_test:main_provider` и `echo_dialogue_test:ambiguity_provider`;
- `/echo_dialogue play echo_dialogue_test:dynamic_provider_showcase` показывает `[Динамический Отец]: Это строка должна сохранить исходный текст.`;
- skin-head берётся из `echo_dialogue_test:dynamic_father`;
- следующая LINE с `dynamic_text` показывает dynamic text-only name без visual slot;
- authored `not_handled`, unavailable, failure, malformed и ambiguous speakers показывают authored fallback;
- CHOICE prompt от `dynamic_father` показывает dynamic name/visual, но choice order не меняется;
- после удаления provider JAR и restart та же recovered session показывает authored fallback без corruption.

Visual-only пункты D6 считаются pending, пока пользователь реально не подтвердит их в клиенте.
