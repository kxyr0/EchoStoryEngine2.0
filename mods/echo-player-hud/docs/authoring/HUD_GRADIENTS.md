# Градиенты HUD

Градиент описывается списком `HudColorStep`.

Каждый step содержит:

- `at` в диапазоне `0..1`;
- ARGB color.

В `0.1.0` renderer использует stepped gradient: выбирается последний step, у которого `at <= normalized`.
Gradient не пересобирается как новый список каждый frame.
