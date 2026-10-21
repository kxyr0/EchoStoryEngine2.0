# C1: Echo Cutscene

Добавлены проекты:

- `mods/echo-cutscene`;
- `mods/echo-dialogue-cutscene-adapter`.

Обновлен `mods/echo-debug` до `0.2.0`.

Ключевая гарантия: detached camera не использует spectator mode. Реальный `ServerPlayer` остается в мире и
удерживается body lock-ом, а клиентский viewport переключается на camera-only marker.

Быстрая проверка:

```text
/echo_debug cutscene enter
/echo_debug cutscene status
/echo_debug cutscene exit
```

Dialogue integration:

```csharp
Cutscene.Play("jdh:chapter_1/bedtime_story");
```

Ручная приемка в Minecraft остается обязательной для визуальной части: автоматические unit/Gradle checks не могут
подтвердить, что игрок видит нужный ракурс и что body pose выглядит правильно в конкретной сцене.
