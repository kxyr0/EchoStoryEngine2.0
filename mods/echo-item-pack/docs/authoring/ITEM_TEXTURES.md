# Item textures

Стандартный путь внешней текстуры:

```text
story/item-pack/<namespace>/assets/textures/item/<name>.png
```

Для `jdh:void_sword`:

```text
story/item-pack/jdh/assets/textures/item/void_sword.png
```

В `.weapon.es` поле выглядит так:

```text
texture: "jdh:item/void_sword"
```

Live reload:

- изменение одного PNG вызывает одну PNG validation;
- EchoScript parser не вызывается;
- binder не вызывается;
- automatic client resource reload is disabled; use an explicit client resource reload or client restart for visual PNG changes;
- broken PNG сохраняет previous known-good texture.

Packaged fallback находится в:

```text
item-forge-1.20.1/src/main/resources/assets/<namespace>/textures/item/
```
