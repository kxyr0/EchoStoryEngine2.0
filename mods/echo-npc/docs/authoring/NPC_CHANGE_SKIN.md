# Npc.ChangeSkin

`Npc.ChangeSkin` меняет только texture ID существующего NPC. `ActorId`, активная
форма, габариты, анимация и body attachments не меняются.

```text
Npc.ChangeSkin(jdh:sasha, jdh:npc/sasha/clothes);
```

Первый аргумент — durable `ActorId`, второй — любой валидный `NpcSkinId` в
формате `namespace:path`. Для примера выше клиент сначала ищет
`assets/jdh/textures/entity/npc/sasha/clothes.png`, затем совместимый путь
`assets/jdh/textures/echo_npc/skins/npc/sasha/clothes.png`.

Вызов проходит через public `NpcService.changeSkin(NpcSkinChangeRequest)`.
Сценарный или диалоговый adapter должен преобразовать два DSL-аргумента в этот
запрос и при необходимости передать стабильный `ActorMutationId`.

Операция durable: сначала записывается Actor Save Schema V4, затем новый skin ID
публикуется в synchronized entity data. Если chunk не загружен, выбранный скин
применится при следующей materialization. Повторный вызов с уже активным ID
идемпотентен и повторно синхронизирует live entity.

Для смены пропорций, collision или semantic body geometry используйте
`Npc.SetForm`. Одежду меняйте через `Npc.ChangeSkin`, а рюкзак и другие предметы
оставляйте независимыми body attachments.

Командный аналог для проверки:

```text
/echo_npc skin set jdh:sasha jdh:npc/sasha/clothes
```
