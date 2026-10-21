# Asset Void Circle

Source file: `D:\JDHSeason\assets\void_circle.png`.

Source size: 256×256 RGBA. Target resource: `echo_void_magic:textures/particle/void_circle.png`. Texture имеет прозрачные углы, мягкую alpha-границу, яркий насыщенно-зелёный центр и глубокий тёмно-зелёный внешний falloff для additive rendering.

Встроенный gradient усиливается runtime tint: `Trail.color = #0F6B32`, `Core.color = #45FF4A`. Size задаётся в `size`, lifetime — в `Ticks`, spacing — в `spacing`, alpha — в `alphaStart/alphaEnd`. Blend mode задаёт общий billboard provider.
