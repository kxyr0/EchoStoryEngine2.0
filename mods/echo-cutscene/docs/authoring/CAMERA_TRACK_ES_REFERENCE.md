# Camera track reference

Минимальный файловый формат версии `0.1.0`:

```text
# time x y z yaw pitch fov
key 0 0 65 0 0 0 70
key 3000 3 65 2 90 0 70
key 6000 0 66 4 180 0 70
```

Файл должен иметь расширение `.camera.es`. Имя файла становится cutscene id в namespace `jdh`.

Пример:

```text
bedtime_story.camera.es -> jdh:bedtime_story
```

Для полного C1 сценария рекомендуется хранить source tracks рядом с dialogue resources и копировать их в world
directory во время authoring.
