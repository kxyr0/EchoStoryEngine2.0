# ADR 0001: Independent Adapter Repository

Статус: принято.

Решение: Echo Story Dialogue Adapter живёт в отдельном repository root `echo-story-dialogue-adapter` и не становится
subproject Echo Story Engine или Echo Dialogue.

Причины:

- сохраняется независимость релизов;
- adapter проверяет только public artifacts;
- sibling implementation classes не попадают в compile classpath;
- isolated runtime ближе к production установке пользователя.

Следствие: acceptance использует build-local Maven repositories, а не Gradle project dependencies.
