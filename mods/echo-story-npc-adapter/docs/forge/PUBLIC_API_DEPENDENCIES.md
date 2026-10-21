# Зависимости Public API

Сборка адаптера использует локальные Maven repositories, опубликованные
сборками соседних проектов:

- `../echo-story-engine/build/integration-maven`;
- `../echo-npc/build/integration-maven`.

Production classpath использует `compileOnly` для публичных artifacts. Tests
используют `testImplementation` для тех же публичных contracts. Проверка
adapter JAR падает, если внутрь попали копии Story/NPC API classes.
