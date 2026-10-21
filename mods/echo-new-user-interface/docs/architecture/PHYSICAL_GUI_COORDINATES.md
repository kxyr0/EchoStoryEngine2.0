# Physical And GUI Coordinates

Источник layout - физический framebuffer:

- `Window.getWidth()`
- `Window.getHeight()`

GUI Scale не участвует в расчёте физического rectangle книги.

Алгоритм:

1. Снять `framebufferWidth`, `framebufferHeight`, `guiWidth`, `guiHeight`, `guiScale`.
2. Рассчитать uniform FIT scale относительно `1920 x 1080`.
3. Рассчитать физические floating edges книги.
4. Округлить края edge-based policy через `Math.round`.
5. Преобразовать физические края в GUI края:
   `guiEdge = physicalEdge * guiSize / framebufferSize`.

Acceptance:

- physical center error `<= 0.5 px`;
- physical rectangle одинаков для GUI Scale 1, 2, 3, 4 и Auto при одном framebuffer;
- GUI rectangle может отличаться, потому что он является вторичным mapping.
