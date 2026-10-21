# Resolution Test Matrix

Pure core tests покрывают обязательные разрешения:

- `1920 x 1080`: scale `1.0`, physical `568,234,784,612`;
- `3840 x 2160`: scale `2.0`, physical `1136,468,1568,1224`;
- `2560 x 1440`;
- `1366 x 768`;
- `1280 x 1024`;
- `3440 x 1440`;
- `1024 x 768`;
- `1080 x 1920`;
- `1919 x 1079`.

Для каждого размера проверяется:

- center error `<= 0.5 physical pixel`;
- clipping отсутствует;
- scale положительный;
- aspect ratio сохраняется с погрешностью raster rounding.
