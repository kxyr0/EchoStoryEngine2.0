# Cutscene errors

Частые ошибки:

- `CUTSCENE_TRACK_NOT_FOUND` - нет definition или camera track для указанного id.
- `CUTSCENE_NOT_PLAYING` - stop вызван без активной cutscene.
- `ALREADY_PLAYING` - для игрока уже есть активный playback.

Проверка:

```text
/echo_cutscene status
/echo_cutscene reload
/echo_debug cutscene status
```

Если Dialogue завис на external wait, проверь:

- установлен ли `echo-dialogue-cutscene-adapter`;
- есть ли `echo_dialogue` и `echo_cutscene` в списке загруженных модов;
- совпадает ли `cutscene` id в payload с id track-а.
