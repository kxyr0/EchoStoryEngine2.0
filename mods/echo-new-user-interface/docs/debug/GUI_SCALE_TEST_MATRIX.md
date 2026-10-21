# GUI Scale Test Matrix

Для framebuffer `1920 x 1080` проверяются GUI sizes:

- GUI Scale 1: `1920 x 1080`;
- GUI Scale 2: `960 x 540`;
- GUI Scale 3: `640 x 360`;
- GUI Scale 4: `480 x 270`;
- Auto: тестируется как GUI `640 x 360` с тем же физическим framebuffer.

Ожидаемый physical rectangle всегда:

`568,234,784,612`

GUI rectangle меняется, но back-mapping в physical edges допускает расхождение не больше `1 px`.
