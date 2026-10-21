# Skins

Skin ID имеет формат `namespace:path`.

## Resource path

Для `sample:father` renderer последовательно ищет:

```text
assets/sample/textures/entity/father.png
assets/sample/textures/echo_npc/skins/father.png
```

Оба пути разрешаются по `NpcSkinId`; таблицы персонажей или названий одежды в
Java-коде нет.

## Dynamic change

`NpcService.changeSkin` принимает `ActorId` и любой `NpcSkinId`. Новый ID
сохраняется в Actor Save Schema V4 до обновления live entity, синхронизируется
клиенту через `SynchedEntityData` и переживает restart/chunk unload.

```text
Npc.ChangeSkin(jdh:sasha, jdh:npc/sasha/clothes);
```

Явный dynamic skin имеет приоритет над skin активной формы и definition. Он не
меняет форму, размеры, animation graph или body attachments.

## Fallback

Если skin отсутствует или не указан, используется:

```text
assets/echo_npc/textures/echo_npc/skins/fallback.png
```

Missing skin диагностируется bounded cache: один warning на skin ID до лимита
кэша, чтобы reload или массовый spawn не засыпали log одинаковыми сообщениями.

## Запреты

Echo NPC не обращается к HTTP skin services, player profiles или external account APIs.
