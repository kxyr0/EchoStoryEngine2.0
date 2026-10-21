# Echo TACZ: Epic Fight Adapter

Standalone Forge adapter for Minecraft 1.20.1 that keeps Epic Fight out of battle mode while a player holds a TACZ gun.

## Problem

TACZ owns the gun handling experience: aiming, firing, reloads, draw animations, first-person hands, and third-person gun presentation. Epic Fight battle mode replaces player combat input and animation behavior, which can break TACZ when both systems are active at the same time.

This adapter does not try to merge the two animation systems. Its rule is intentionally narrow:

```text
Held TACZ IGun -> Epic Fight PlayerMode.VANILLA
```

## Supported Versions

- Minecraft: `1.20.1`
- Forge: `47.4.10`
- Java: `17`
- TACZ: `1.1.8-hotfix`
- Epic Fight: `20.14.17`

## Dependencies

`mods.toml` requires:

- `forge`
- `minecraft`
- `tacz`
- `epicfight`

Epic Fight is resolved from Modrinth Maven. TACZ is resolved from the target Prism instance `mods` directory as an external deobfuscated dependency. The adapter jar does not shade, jarJar, shadow, or copy TACZ or Epic Fight classes.

## How It Works

Epic Fight posts `BattleModeSustainableEvent` from `PlayerPatch#tick` while a player is in battle mode or while battle mode is temporarily restricted. If that event is canceled, Epic Fight's own code calls `toVanillaMode(false)` and marks `battleModeRestricted = true`. When the event stops being canceled, Epic Fight clears that restriction and returns to `toEpicFightMode(false)`.

The adapter registers common-side Forge event handlers:

```java
@SubscribeEvent(priority = EventPriority.HIGHEST)
public static void onBattleModeSustainable(BattleModeSustainableEvent event)
```

This handler cancels the event when the original player is holding a TACZ gun.

Version `0.1.1` adds two guards for the installed Epic Fight `20.14.17` behavior:

- `ChangePlayerModeEvent` is canceled when TACZ gun is held and Epic Fight attempts to enter `PlayerMode.EPICFIGHT`.
- `TickEvent.PlayerTickEvent` verifies the final player mode at end of tick. If a held `IGun` still leaves the patch in Epic Fight mode, the adapter calls `toVanillaMode(false)` and records that UUID as adapter-restricted on the current logical side. When the gun is no longer held, only players restricted by this adapter are restored with `toEpicFightMode(false)`.

The tick guard exists because `LocalPlayerPatch.toEpicFightMode(true)` in this Epic Fight version sends its mode packet and adjusts client UI/camera before `ChangePlayerModeEvent` can cancel the mode assignment.

## Gun Detection

The detector checks the active held item stacks only:

```java
IGun.getIGunOrNull(stack) != null
```

TACZ's own helper resolves to the same `instanceof IGun` check in `1.1.8-hotfix`. The adapter checks both main hand and offhand. It does not inspect inventory, registry namespace, item ids, NBT, JSON gun data, or specific TACZ implementation classes, so addon guns that correctly implement `IGun` are supported.

## Mode Restore

The preferred restoration path is Epic Fight's built-in `battleModeRestricted` flow:

- If the player was in Epic Fight mode and equips a TACZ gun, Epic Fight exits to vanilla mode.
- If the player removes the gun, Epic Fight restores battle mode.
- If the player was already in vanilla mode before holding the gun, the event is not involved until Epic Fight attempts to sustain or re-enter battle mode, so the adapter does not force Epic Fight back on afterward.

The `0.1.1` tick guard has its own UUID-side fallback set only for players it actually forced out of Epic Fight mode. This preserves the same rule: players that were already in vanilla mode are not forced into Epic Fight after removing the gun.

## Build

```text
gradlew build
```

The build verifies that the adapter jar contains only adapter classes and resources, not TACZ, Epic Fight, Minecraft, or Forge classes.

## Test Checklist

Test with TACZ, Epic Fight, and this adapter installed:

- Epic Fight mode with a normal item.
- Switch to a TACZ gun in main hand.
- Confirm player mode becomes vanilla.
- Fire, ADS, reload, draw, inspect, and fire-mode actions.
- Press Epic Fight battle mode key while holding the gun.
- Switch TACZ gun to another TACZ gun without battle mode flashing back.
- Switch from TACZ gun to a normal weapon and confirm Epic Fight restores only if it was previously active.
- Keep TACZ gun only in inventory and confirm Epic Fight still works.
- Offhand TACZ gun behavior.
- Death, respawn, dimension change, and world rejoin.
- Dedicated server with two players, only one holding a TACZ gun.
- TACZ addon gun implementing `IGun`.

## Known Limitations

No mixin is included. The adapter now uses event cancellation plus an end-of-tick mode enforcer. Add a client-only mixin/input guard only if real gameplay still shows a visible one-tick camera/UI flicker or an Epic Fight action slipping through while a TACZ gun is held.
