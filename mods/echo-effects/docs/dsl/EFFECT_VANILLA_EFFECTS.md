# Effect Vanilla Effects

`VanillaEffect.Apply(...)` добавляет обычный ванильный `MobEffect` Minecraft на игрока, на котором активен текущий Echo Effects эффект.

Рабочая сигнатура:

```csharp
VanillaEffect.Apply(
    time: Seconds(0.0),
    effect: "minecraft:nausea",
    duration: Seconds(7.0),
    amplifier: 0,
    ambient: false,
    visible: true,
    showIcon: true
);
```

Короткая форма:

```csharp
VanillaEffect.Apply(effect: "minecraft:nausea", duration: Seconds(7));
```

`time` можно не указывать: тогда эффект применяется на старте instance.

`duration` обязательна и задается через `Seconds(...)` или `Ticks(...)`.

`amplifier: 0` означает vanilla уровень I, `amplifier: 1` означает уровень II.

Примеры:

```csharp
VanillaEffect.Apply(effect: "minecraft:blindness", duration: Seconds(2), showIcon: false);
VanillaEffect.Apply(effect: "minecraft:slowness", duration: Seconds(5), amplifier: 1);
VanillaEffect.Apply(effect: "minecraft:weakness", duration: Ticks(80));
```

`minecraft:nausea` поддерживается для авторов контента. На Forge 1.20.1 runtime также учитывает старый внутренний alias `minecraft:confusion`.

`VanillaEffect.Apply` является one-shot timeline event: один active instance применяет один payload только один раз. При `REFRESH_DURATION` ledger сбрасывается.
