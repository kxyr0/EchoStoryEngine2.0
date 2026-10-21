# Detached camera

Detached camera реализована только на клиенте:

- создается `net.minecraft.world.entity.Marker`;
- marker не добавляется в серверный world и не отправляется по сети;
- `Minecraft#setCameraEntity(marker)` переключает viewport;
- `Minecraft#setCameraEntity(player)` возвращает камеру в тело.

Это намеренно не spectator mode. GameMode игрока не меняется, поэтому не ломаются инвентарь, Wardrobe state,
серверные permissions и логика других модов.

FOV берется из `CameraPose.fieldOfView()` и применяется через `ViewportEvent.ComputeFov`.
