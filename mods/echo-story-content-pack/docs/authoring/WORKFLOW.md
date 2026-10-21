# Authoring workflow

1. Создайте или измените `.npc.es`.
2. Экспортируйте NPC skin PNG 64x64.
3. Создайте или измените `.dialogue.es`.
4. Экспортируйте optional portrait PNG.
5. Создайте или измените Story `.es`.
6. Запустите `.\gradlew.bat validateContent`.
7. Запустите `.\gradlew.bat validateWithEchoRuntime`.
8. Соберите JAR: `.\gradlew.bat build`.
9. Соберите staged pack: `.\gradlew.bat stageStoryPack`.
10. Скопируйте staged `mods` в clean Forge instance.
11. Запустите Minecraft или dedicated server.
12. Проверьте catalogs командами:
    - `/echo_story health`
    - `/echo_story stories`
    - `/echo_story diagnostics 20`
    - `/echo_dialogue health`
    - `/echo_dialogue dialogues`
    - `/echo_dialogue providers`
    - `/echo_npc health`
    - `/echo_npc definitions`

`installContentJar` копирует только content pack JAR:

```powershell
.\gradlew.bat installContentJar -PinstanceDir=<path>
```

Эта задача не копирует sibling runtime mods, не запускает Minecraft, не меняет saves и не принимает EULA.

