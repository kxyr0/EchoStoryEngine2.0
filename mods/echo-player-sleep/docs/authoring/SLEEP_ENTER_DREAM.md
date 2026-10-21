# EnterDream

`Sleep.EnterDream` разрешён только после `EYES_CLOSED`.

Runtime проверяет:

- есть active session;
- игрок online;
- глаза закрыты по server state;
- целевое измерение загружено.

Только после этого вызывается `ServerPlayer.stopSleepInBed` и телепорт настоящего player. Fake player, spectator mode и копия тела не используются.

Same-dimension и cross-dimension destination проходят через один typed `PlayerSleepDreamDestination`.
