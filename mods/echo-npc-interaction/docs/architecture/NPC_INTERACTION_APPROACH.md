# NPC Interaction Approach

Provider формирует candidates:

- standing position;
- facing direction;
- hit point;
- block face;
- required reach;
- clearance;
- line-of-sight requirement;
- estimated approach cost.

Resolver и navigation adapter проверяют collision, headroom, reach, line of
sight и достижимость через Echo Navigation. AUTO face выбирается из выбранного
approach, а не случайно.

NI1 не ищет ближайший блок и не заменяет target поблизости.

