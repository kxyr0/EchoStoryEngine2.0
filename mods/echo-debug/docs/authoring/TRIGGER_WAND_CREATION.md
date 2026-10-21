# Trigger Wand: создание trigger

Экран создания открывается клавишей Enter только если authoritative selection complete.

## Поля

- Namespace: default `jdh`.
- Trigger name/path: обязательное поле, например `chapter_1/house_exit`.

Class name вычисляется из последнего сегмента path. Для `chapter_1/house_exit` это `HouseExit`.

## Pipeline

Сервер выполняет цепочку:

```text
selection snapshot
-> TriggerId/path planning
-> source rendering
-> TriggerSourceValidationService.validate
-> atomic write
-> TriggerSourceReloadService.reload
-> catalog contains expected TriggerId
-> client success ACK
```

Echo Debug не импортирует EchoScript. Валидация идёт через Echo Trigger public authoring service,
который внутри вызывает настоящий EchoScript parser и Trigger binder.

## Rollback

Если reload или catalog verification не прошли после записи, Echo Debug удаляет созданный final file.
При validation failure файл не создаётся.
