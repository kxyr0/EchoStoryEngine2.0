# Диагностические команды

## Дерево команд

Корень команд только один:

```text
/echo_story
```

Зарегистрированные read-only подкоманды:

```text
/echo_story health
/echo_story stories
/echo_story executions
/echo_story execution <executionId>
/echo_story extensions
/echo_story extension <extensionId>
/echo_story diagnostics
/echo_story diagnostics <limit>
```

Право доступа: `Commands.LEVEL_GAMEMASTERS`.

`diagnostics` без аргумента использует лимит `10`. Явный лимит принимает только целые значения `1..50`.
`0`, отрицательные, malformed и значения больше `50` отклоняются русским сообщением.

## Примеры

```text
/echo_story health
/echo_story stories
/echo_story executions
/echo_story execution sample:execution
/echo_story extensions
/echo_story extension echo_story_test:extension
/echo_story diagnostics
/echo_story diagnostics 20
```

## Источники данных

Команды читают только:

- bounded `InMemoryDiagnosticBuffer`;
- текущий `HostSnapshot` и `HostHealth`;
- текущий `CompiledStoryCatalogSnapshot`.

Команды не запускают execution, не делают reload, не меняют Story state, не пишут отдельные log-файлы, не печатают
полные variables, raw checkpoints, stack traces, source text или абсолютные storage paths.

## Формат вывода

Вывод операторских команд написан читаемым русским UTF-8. Stable API values, enum values, ids и diagnostic codes
сохраняются как идентификаторы.

`health` сообщает lifecycle state host, статус и ревизию каталога, число compiled story, active/waiting executions,
заполнение persistence queue, заполнение server work queue, latest reload и recent warning/error count.

`stories` возвращает bounded sorted список: Story ID, короткий compiled fingerprint и logical source ID.

`executions` возвращает bounded sorted список: execution ID, Story ID, phase, current node и pending action type,
если он есть.

`execution <executionId>` валидирует namespaced ID, показывает одну bounded summary и возвращает русское not-found
сообщение, если execution отсутствует.
