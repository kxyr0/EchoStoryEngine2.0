# `.weapon.es` reference

Обязательные поля:

- `id`: строка `namespace:path`, например `jdh:void_sword`.
- `name`: непустая строка.
- `texture`: строка resource path, например `jdh:item/void_sword`.

Optional поля и defaults:

- `description`: строка, default `""`.
- `tooltip`: массив строк, default `[]`.
- `model`: строка, default generated handheld model.
- `damage` или `attackDamage`: число, default `1`.
- `attacksPerSecond`: число, default `1`.
- `attackSpeedModifier`: число Minecraft modifier; конфликтует с `attacksPerSecond`.
- `knockback`: число, default `0`.
- `durability`: integer. If omitted, the weapon is unbreakable.
- `unbreakable`: boolean, default `true` when `durability` is omitted, otherwise `false`.
- `enchantability`: integer, default `0`.
- `repairIngredient`: строка, default отсутствует.
- `rarity`: `COMMON`, `UNCOMMON`, `RARE`, `EPIC`; default `COMMON`.
- `glint`: boolean, default `false`.
- `fireResistant`: boolean, default `false`.
- `equipmentSlot`: `HEAD`, `CHEST`, `LEGS`, or `FEET`; default none. When set, Forge allows the item to be equipped in that slot.
- `hiddenFromCreative`: boolean, default `false`.
- `creativeOrder`: integer, default сортировка по Item ID.

ID rules:

- namespace и path обязательны;
- только lowercase;
- namespace `minecraft` запрещен для Echo items;
- `void_sword.weapon.es` должен содержать ID с последним segment `void_sword`;
- path traversal через `..` отклоняется.

Стабильные error codes:

- `ITEM_SCRIPT_PARSE_ERROR`
- `ITEM_SCRIPT_BIND_ERROR`
- `INVALID_ITEM_ID`
- `DUPLICATE_ITEM_ID`
- `REGISTRY_ID_CONFLICT`
- `ITEM_KIND_NOT_FOUND`
- `ITEM_KIND_CHANGE_REQUIRES_RESTART`
- `NEW_ITEM_REQUIRES_RESTART`
- `ITEM_SOURCE_REMOVED`
- `ITEM_DEFINITION_UNAVAILABLE`
- `INVALID_ATTACK_DAMAGE`
- `INVALID_ATTACK_SPEED`
- `INVALID_DURABILITY`
- `INVALID_ATTRIBUTE`
- `ATTRIBUTE_CONFLICT`
- `TEXTURE_NOT_FOUND`
- `TEXTURE_DECODE_FAILED`
- `MODEL_PARSE_FAILED`
- `MODEL_REFERENCE_INVALID`
- `LAST_KNOWN_GOOD_RETAINED`
- `CLIENT_REVISION_GAP`
- `MANIFEST_WRITE_FAILED`

Все documented examples проверяются тестом `DocumentedExamplesTest` через настоящий EchoScript parser
и `WeaponDefinitionBinder`.
