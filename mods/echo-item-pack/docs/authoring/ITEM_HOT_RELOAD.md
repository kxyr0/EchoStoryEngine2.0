# Item hot reload

Reload batch применяется атомарно:

```text
read changed files
compile immutable candidates
validate candidates
build immutable catalog candidate
reject batch on ERROR
publish catalog revision
refresh runtime values
sync clients
```

В production runtime сервер дополнительно выполняет periodic reconciliation через `ItemPackReloadTicker` каждые `40`
server ticks. Это не регистрирует новые Item ID после registry freeze: новые ID попадают в pending restart, а уже
существующие `EchoWeaponItem` получают обновлённые значения из нового immutable catalog.

Client catalog sync and server hot reload do not call `Minecraft.reloadResourcePacks()`. Texture/model visuals are refreshed only after an explicit client resource reload or client restart.

Live-reloadable properties:

- display name;
- description;
- tooltip lines;
- damage;
- attack speed;
- knockback;
- durability;
- enchantability;
- repair ingredient;
- rarity;
- glint;
- fire resistance;
- texture definition reference; visual refresh requires an explicit client resource reload or client restart;
- model definition reference; visual refresh requires an explicit client resource reload or client restart;
- creative ordering existing item.

Restart-required properties:

- новый Item ID;
- rename Item ID;
- namespace change;
- item kind change;
- окончательное удаление registry entry.

Last-known-good:

- broken `.weapon.es` не заменяет active definition;
- broken PNG не заменяет active texture;
- broken model не заменяет active model;
- диагностика bounded и доступна через команды.
