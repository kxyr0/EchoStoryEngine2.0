# Карта проекта

- `src/main/java/io/github/kxyr0/echostorynpcadapter`
  - Entrypoint Forge-мода.
- `src/main/java/io/github/kxyr0/echostorynpcadapter/action`
  - Канонические action IDs и разбор payload.
- `src/main/java/io/github/kxyr0/echostorynpcadapter/mutation`
  - Детерминированное соответствие `ActionExecutionId` -> `ActorMutationId`.
- `src/main/java/io/github/kxyr0/echostorynpcadapter/observe`
  - Ограниченные observations и проверки NPC postconditions.
- `src/main/java/io/github/kxyr0/echostorynpcadapter/story`
  - Story extension factory, session, handlers и start/recover dispatch.
- Echo NPC N5 form actions
  - `Npc.SetForm` and `Npc.PlaceAt` must keep the same `ActorId`; story state
    should not model child/adult forms as different actors.
- `src/test/java`
  - Простые JVM contract tests.
- `src/architectureTest/java`
  - Проверки границ зависимостей.
