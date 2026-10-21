# ADR 0001: Независимый адаптер

Статус: принято для N3.

Echo Story NPC Adapter является отдельным репозиторием Forge-мода. Он
использует публичные artifacts Story и Echo NPC вместо implementation modules.

Это сохраняет независимый release-путь Story Engine и Echo NPC и не дает
адаптеру стать скрытым мостом implementation-классов.
