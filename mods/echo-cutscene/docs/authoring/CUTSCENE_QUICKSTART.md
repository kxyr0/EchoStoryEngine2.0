# Быстрый старт

1. Положи `echo-cutscene` рядом с остальными модами Echo Platform.
2. Для проверки запусти world с `echo-debug` версии `0.2.0`.
3. Введи:

```text
/echo_debug cutscene enter
/echo_debug cutscene status
/echo_debug cutscene exit
```

Встроенный preview id:

```text
jdh:chapter_1/bedtime_story
```

Свой track можно положить сюда:

```text
<world>/echo/cutscenes/my_scene.camera.es
```

После изменения файла выполни:

```text
/echo_cutscene reload
/echo_cutscene play jdh:my_scene
```
