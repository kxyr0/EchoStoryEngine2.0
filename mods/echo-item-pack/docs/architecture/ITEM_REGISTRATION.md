# Item registration architecture

Forge registry замораживается после startup. Поэтому Echo Item Pack разделяет identity и live definition.

Startup:

```text
packaged Item IDs
+ external Item IDs
+ manifest Item IDs
= registered EchoWeaponItem objects
```

Каждый ID получает generic Java object:

```text
EchoWeaponItem(EchoItemId)
```

`EchoWeaponItem` не хранит damage/name/texture. Он читает их из active immutable catalog.

Machine-owned manifest:

```text
<gameDir>/config/echo-item-pack/registered-items.json
```

Manifest хранит:

- Item ID;
- item kind;
- first seen version;
- last seen source fingerprint;
- pending registration flag;
- deprecated flag.

Если definition отсутствует, registry entry все равно создается из manifest, а runtime использует
safe unavailable definition, чтобы не уничтожать существующие ItemStack.

Новый item во время игры не регистрируется после freeze. Он получает pending restart diagnostic и
появляется после следующего запуска.
