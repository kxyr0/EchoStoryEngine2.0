# Wearable models

Wearable models are optional item model descriptors attached to a wearable. They do not change the resolved 64x64 skin. They are extra rendered geometry, mostly useful for glasses.

```es
Model("jdh:item/origin_john_glasses");
ModelPosition(0, 0, 0);
ModelRotation(0, 0, 0);
ModelScale(1, 1, 1);
```

The model resource is a standard Minecraft model ID, backed by `assets/<namespace>/models/item/...json` and normal model textures. Transform values are finite doubles; scale must be positive.
