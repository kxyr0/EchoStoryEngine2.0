# Item models

Custom model optional. Внешний путь:

```text
story/item-pack/<namespace>/assets/models/item/<name>.json
```

Если model отсутствует, runtime предоставляет virtual generated model:

```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "layer0": "<namespace>:item/<texture-name>"
  }
}
```

Generated JSON не записывается в authoring directory. Он принадлежит virtual client resource pack.

Если custom model существует:

- JSON должен парситься;
- `parent` и texture references должны быть валидными ResourceLocation;
- broken model сохраняет previous known-good model;
- если previous отсутствует, используется generated handheld fallback.
