# Экспорт neutral skin

Neutral export записывает 64x64 PNG внутри настроенного workspace.

Exporter копирует пиксели точно и не использует интерполяцию. Вывод за пределы workspace отклоняется.
Любой слой, пиксели которого выходят за 64x64 skin atlas, отклоняется вместо тихого clipping.
Порядок слоев является авторитетным: более поздние visible layers перезаписывают более ранние в том же пикселе.

Канонический production path:

`src/main/resources/assets/jdh/textures/echo_npc/skins/`
