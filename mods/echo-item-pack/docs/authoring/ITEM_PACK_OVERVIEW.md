# Echo Item Pack: обзор authoring

Echo Item Pack позволяет добавлять оружие через `.weapon.es` без Java-класса на каждый
предмет. На первом этапе поддержан только kind `echo_item:weapon`.

Внешний путь вычисляется от gameDir:

```text
<gameDir>/story/item-pack/
```

Текущий пользовательский путь:

```text
C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Форма Мысли\minecraft\story\item-pack\
```

Минимальная структура:

```text
story/item-pack/jdh/items/weapons/swords/void_sword.weapon.es
story/item-pack/jdh/assets/textures/item/void_sword.png
```

После restart предмет регистрируется как `jdh:void_sword`. Если новый файл добавлен уже
после freeze registry, команда `/echo_item pending` покажет `PENDING_REGISTRATION_RESTART`.

Рабочий пример из packaged test content:

```text
namespace EchoItemPack.Jdh.Items.Weapons;

class VoidSword : WeaponResource
{
    public override WeaponDefinition Build()
    {
        return Weapon(
            id: "jdh:void_sword",
            name: "Void Sword",
            description: "A test blade loaded from EchoScript.",
            tooltip: ["Damage and speed can hot reload.", "Texture is resolved from the item-pack assets."],
            texture: "jdh:item/void_sword",
            model: "handheld",
            damage: 13,
            attacksPerSecond: "1.6",
            durability: 2031,
            enchantability: 18,
            rarity: EPIC,
            glint: true,
            fireResistant: true,
            equipmentSlot: CHEST,
            creativeOrder: 10
        );
    }
}
```

Каждая строка:

- `namespace` нужен EchoScript parser как синтаксическая область; item ID берется не отсюда.
- `class ... : WeaponResource` сообщает binder, что файл описывает weapon resource.
- `Build()` обязан вернуть ровно один `Weapon(...)`.
- `id` задает registry identity и должен совпадать с именем файла.
- `name`, `description`, `tooltip` управляют отображением.
- `texture` указывает `<namespace>:item/<name>` без `.png`.
- `model: "handheld"` включает стандартную handheld-модель.
- `damage`, `attacksPerSecond`, `durability` управляют боевыми свойствами.
- `rarity`, `glint`, `fireResistant`, `equipmentSlot`, `creativeOrder` являются live definition fields.

Команды:

- `/echo_item reload`
- `/echo_item validate`
- `/echo_item inspect <item_id>`
- `/echo_item sources <item_id>`
- `/echo_item pending`
- `/echo_item give <player> <item_id> [count]`
