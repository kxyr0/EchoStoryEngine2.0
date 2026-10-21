# Portraits D5

`Portrait("sample:merchant")` resolves client-side to:

```text
assets/sample/textures/echo_dialogue/portraits/merchant.png
```

Rules:

- the source contains only a namespaced ID;
- the resolver appends `.png`;
- PNG bytes are not sent over the network;
- missing or corrupted PNG falls back to text-only;
- alpha is supported;
- normal portraits preserve aspect ratio inside the bounded square.

Compatibility rule: if a `Portrait(...)` texture is exactly `64x64`, Echo Dialogue treats it as a
Minecraft skin sheet and renders only the head UVs, the same as `SkinHead(...)`. This preserves older
content where dialogue portraits were authored as skins plus an Echo Animation face overlay.

Recommended source size for drawn portraits is `128x128` or `256x256`. Rectangular PNGs are allowed
because the renderer scales them with aspect ratio preserved.
