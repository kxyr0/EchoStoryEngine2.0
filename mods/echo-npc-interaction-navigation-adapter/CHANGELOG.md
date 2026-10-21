# Changelog

## 0.2.0

- Добавлены typed причины отклонения approach candidates: support, collision, headroom, reach, LOS и dimension.
- LOS принимает raycast hit в сам target block как допустимое взаимодействие с этим target.
- `/echo_npc_interaction approaches` получает подробный breakdown через public execution port diagnostics.

## 0.1.0

- Добавлен Forge adapter для `NpcInteractionExecutionPort`.
- Добавлен выбор approach через collision/headroom/line-of-sight проверки.
- Добавлен запуск Echo Navigation через active backend из public query service.
- Добавлена отмена owned `NavigationMoveId`.
- Добавлен `sourceShapeCheck`.
