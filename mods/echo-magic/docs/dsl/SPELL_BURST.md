# Burst

`Burst(count: 3, interval: Ticks(2))` выпускает три логических projectile с интервалом два server ticks. Scheduler ограничивает число emissions за tick и сохраняет порядок при лаге.

Cast завершается только после конца burst и terminal-состояния всех projectile.

Опциональный `spread` превращает burst в веер:

```csharp
Burst(count: 9, interval: Ticks(1), spread: Degrees(100));
```

Первый projectile идёт по левому краю, последний — по правому, остальные равномерно распределяются между ними. Без `spread` поведение остаётся прежним.
