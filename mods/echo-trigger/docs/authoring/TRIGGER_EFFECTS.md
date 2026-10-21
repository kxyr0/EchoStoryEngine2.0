# Эффекты

Predicates:

- `HasEffect("minecraft:poison")`
- `EffectAmplifierAtLeast("minecraft:strength", 1)`
- `EffectDurationAtLeast("minecraft:poison", 200)`

Amplifier uses the vanilla value: `0` is level I, `1` is level II.

Events:

- `EffectAdded(effectId, amplifierAtLeast: 1)`
- `EffectRemoved(effectId)`
- `EffectAmplifierChanged(effectId)`

Payload contains effect ID, previous amplifier, new amplifier and duration. Source entity is not stored.
