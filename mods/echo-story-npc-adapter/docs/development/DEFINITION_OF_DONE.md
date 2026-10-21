# Критерии готовности

N3 считается завершенным только когда:

- adapter repo независим;
- production code использует только публичные Story/NPC artifacts;
- зарегистрированы три канонических action types;
- spawn/teleport/despawn валидируют arguments и отправляют детерминированные
  mutations;
- recovery работает для задокументированных crash windows;
- completion outputs точные и не содержат entity UUID;
- adapter JAR не включает копии Story/NPC API или test support;
- проходят JVM tests, architecture tests, JAR verification и source-shape
  checks;
- Forge GameTests и isolated final-JAR acceptance выполнены либо явно указаны
  как невыполненные с причиной.
