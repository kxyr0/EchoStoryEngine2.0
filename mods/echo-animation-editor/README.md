# Echo Animator Editor

Echo Animator Editor (`echo_animation_editor`) - отдельный клиентский Forge 1.20.1 мод для первого среза
внутриигрового редактора Echo Animation.

## Что реализовано в 0.3.0

- самостоятельный Gradle-репозиторий `mods/echo-animation-editor`;
- neutral `animation-editor-contracts` без Minecraft/Forge импортов;
- plain Java `animation-editor-core` с physical framebuffer layout, GUI mapping, timeline geometry,
  draft state and diagnostics;
- client command `/echo_animator open|close|toggle|save|autosave|hot_reload|undo|redo|recover|status`;
- non-pausing `AnimatorEditorScreen` поверх мира;
- client-only detached camera через `Marker` и `Minecraft#setCameraEntity`;
- восстановление исходной camera entity, perspective и HUD flag при закрытии;
- HUD suppression во время активной editor session;
- тонкие adapter-boundaries для public Echo Animation/NPC bridge-классов без копирования runtime/parser;
- optional reflection bridge к `echo-animation-authoring` public draft API;
- viewport LMB drag работает через picked gizmo handle для axes, planes, rotate rings and scale handles;
- live discovery выбирает доступные bone/face ids через authoring/Echo Animation public surface;
- undo/redo выполняются внутри authoring draft history;
- autosave/recovery пишут и читают authoring sidecar, не production `.animation-clip.es`;
- preview hook включает dev-only authoring preview override state;
- viewport mutation создает или заменяет keyframe mutation для выбранной кости;
- Ctrl+S и `/echo_animator save` сохраняют draft через authoring API;
- `/echo_animator hot_reload` вызывает authoring hot reload hook, а реальный reload выполняет watcher Echo Animation.

## Команды

- `/echo_animator open`
- `/echo_animator open <animation_subject_id>`
- `/echo_animator open <animation_subject_id> <animation_id>`
- `/echo_animator close`
- `/echo_animator toggle`
- `/echo_animator save`
- `/echo_animator autosave`
- `/echo_animator hot_reload`
- `/echo_animator undo`
- `/echo_animator redo`
- `/echo_animator recover`
- `/echo_animator preview`
- `/echo_animator clear_preview`
- `/echo_animator mode body`
- `/echo_animator mode face`
- `/echo_animator select bone <bone_id>`
- `/echo_animator select face <face_element_id>`
- `/echo_animator status`

## Оставшиеся ограничения

Этот срез закрывает реальные editor routes для save, keyframe mutation, hot reload hook, rich gizmo picking,
bone/face discovery, undo/redo, autosave/recovery и preview hook. Ручной Minecraft acceptance всё ещё нужно выполнить
в интерактивном клиенте: я проверил автоматические тесты и сборку, но не запускал игровой UI вручную.
