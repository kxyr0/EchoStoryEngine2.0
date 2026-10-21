# Отключение hunger

Echo Player Vitals переводит vanilla hunger в compatibility state и не использует его как игровой ресурс.

Целевая политика:

- food level удерживается как совместимое значение;
- exhaustion сбрасывается;
- starvation damage не должен происходить;
- sprint не должен зависеть от hunger;
- food-based natural regeneration не считается игровой системой Vitals.

HUD-иконки hunger скрывает `echo-player-hud`, а не Vitals. Это намеренное разделение: Vitals отвечает за механику,
HUD отвечает за отображение.

Проверка в игре:

```text
/echo_vitals hunger status <player>
```

Скрытые иконки hunger не являются доказательством отключённой механики. Проверять нужно отсутствие food decrease,
exhaustion accumulation, starvation damage и food-based regeneration.
