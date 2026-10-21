# Echo Debug

Development-only Trigger Wand authoring tool for Echo Platform.

Version: `0.5.0`

Режим `SEAT` создаёт триггер существующего события Echo Seating: игрок сел на конкретное сиденье.
Переключите режим клавишей V (из BOX можно сразу Shift+V), выберите сиденье ЛКМ,
нажмите Enter и укажите название триггера и ID истории (`namespace:path`).
Выбирать можно блок под anchor сиденья или сам блок anchor; ПКМ отменяет точку.
Нужны зарегистрированные слоты Echo Seating и Echo Trigger Seating Adapter.
При отсутствии сиденья или неоднозначном выборе файл не создаётся.

Созданный `.trigger.es` использует `Player.SeatedAt(Zone, Seat)`, `StartStory(...)`
и `FireOnlyOncePerPlayer()`. Повторное срабатывание можно включить, убрав последнее правило
из файла. История выполняется существующим адаптером, debug-мод только создаёт и проверяет исходник.
