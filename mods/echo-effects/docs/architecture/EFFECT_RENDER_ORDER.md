# Effect Render Order

Screen channels имеют `ScreenLayer`: `BELOW_UI` и `ABOVE_UI`.

Forge overlay должен покрывать весь framebuffer и не зависеть от GUI Scale. В текущем renderer fill использует width/height overlay callback-а.
