# Скины игрока

Base appearance в runtime всегда нормализуется в один authored resource skin:

- `echo_content_player:player/base`;
- `PlayerSkinModel.CLASSIC`;
- `assets/echo_content_player/textures/echo_player/skins/player/base.png`.

Vanilla account skin не сохраняется и не используется как fallback. Если старое сохранение или старый шаблон
содержит `ACCOUNT` или чужой `RESOURCE`, профиль при загрузке/сохранении нормализуется в base skin.

Пример:

```text
PlayerSkinId: echo_content_player:player/base
Resource: echo_content_player:textures/echo_player/skins/player/base.png
File: assets/echo_content_player/textures/echo_player/skins/player/base.png
```

Для authored templates можно указывать `skinMode: RESOURCE`, `skinId` и `skinModel`, но runtime все равно
публикует только base skin.

Проверка custom skin:

- PNG signature;
- успешное декодирование;
- строго 64x64;
- отсутствие path traversal;
- допустимый namespace/path;
- fingerprint source.

Если внешний custom skin сломан или не совпадает с base policy, он не становится persistent appearance.
