# Dedicated Server Testing

Обычная dedicated-server acceptance требует ручного принятия Minecraft EULA.
Нельзя автоматически записывать `eula=true`.

## Проверка

1. Собрать мод:

```powershell
.\gradlew.bat build --console=plain
```

2. Подготовить Forge 1.20.1 server с `forge_version=47.4.10`.
3. Вручную принять EULA, если пользователь согласен.
4. Положить production JAR:

```text
npc-forge-1.20.1/build/libs/echo-npc-0.1.0-SNAPSHOT-all.jar
```

5. Запустить server.
6. Проверить, что нет client-only class loading.
7. Выполнить `/reload`, `/echo_npc health`, `/echo_npc definitions`.
8. Остановить server штатной командой `stop`.

Acceptance можно считать пройденной только после actual start, clean stop и
exit code zero.
