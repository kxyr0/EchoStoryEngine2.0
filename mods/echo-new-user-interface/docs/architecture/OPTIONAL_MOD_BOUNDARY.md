# Optional Mod Boundary

`echo-new-user-interface` является самостоятельным optional модом.

Запрещённые связи отсутствуют:

- `echo-new-user-interface -> echo-dialogue`: нет;
- `echo-dialogue -> echo-new-user-interface`: нет;
- required dependency существующих Echo-модов на новый мод: нет.

Удаление production JAR нового UI должно удалять только:

- команду `/echo_new_ui debug book`;
- overlay `echo_new_user_interface:journal_book`;
- texture книги;
- HUD suppression этого preview.

Старый Dialogue UI, NineSliceRenderer и debug screen не затронуты.
