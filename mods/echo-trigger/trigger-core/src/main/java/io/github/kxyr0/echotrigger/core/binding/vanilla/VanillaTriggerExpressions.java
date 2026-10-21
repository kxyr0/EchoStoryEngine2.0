package io.github.kxyr0.echotrigger.core.binding.vanilla;

import io.github.kxyr0.echotrigger.api.event.TriggerEventKind;
import io.github.kxyr0.echotrigger.api.expression.TriggerExpressionKind;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class VanillaTriggerExpressions {
    private static final Set<String> PREDICATES = Set.of(
            "PlayerInside",
            "HasItem", "ItemCountAtLeast", "ItemCountAtMost", "ItemCountExactly", "HasEmptyInventorySlot",
            "Holding", "HoldingMainHand", "HoldingOffHand",
            "Wearing", "HasAnyArmor", "HasFullArmorSet",
            "HealthAtMost", "HealthAtLeast", "FoodAtMost", "FoodAtLeast",
            "IsSneaking", "IsSprinting", "IsSwimming", "IsSleeping", "IsOnGround", "IsBurning",
            "HasEffect", "EffectAmplifierAtLeast", "EffectDurationAtLeast",
            "AdvancementCompleted", "ExperienceLevelAtLeast",
            "InDimension", "GameModeIs", "TimeBetween", "DayAtLeast", "WeatherIs");

    private static final Set<String> EVENTS = Set.of(
            "PlayerEnter", "PlayerExit", "PlayerStay",
            "ItemAdded", "ItemRemoved", "ItemPickedUp", "ItemDropped", "ItemUsed", "ItemConsumed",
            "InventoryBecameFull", "InventorySlotFreed",
            "HeldItemChanged", "MainHandChanged", "OffHandChanged",
            "EquipmentChanged", "EquipmentEquipped", "EquipmentUnequipped",
            "PlayerDamaged", "PlayerHealed", "PlayerDied", "PlayerRespawned", "PlayerJumped",
            "SneakStarted", "SneakStopped", "SprintStarted", "SprintStopped",
            "SwimmingStarted", "SwimmingStopped", "SleepStarted", "SleepFinished",
            "PlayerStartedBurning", "PlayerStoppedBurning",
            "EffectAdded", "EffectRemoved", "EffectAmplifierChanged",
            "ExperienceLevelReached", "AdvancementGranted",
            "DimensionChanged", "DimensionEntered", "DimensionLeft", "GameModeChanged",
            "BlockInteracted", "BlockBroken", "BlockPlaced", "ContainerOpened", "BedEntered", "BedLeft",
            "TimeReached", "DayReached", "WeatherChanged");

    private VanillaTriggerExpressions() {
    }

    public static Optional<TriggerExpressionKind> kind(String name) {
        if (PREDICATES.contains(name)) {
            return Optional.of(TriggerExpressionKind.PREDICATE);
        }
        if (EVENTS.contains(name)) {
            return Optional.of(TriggerExpressionKind.EVENT);
        }
        return Optional.empty();
    }

    public static TriggerEventKind eventKind(String name, TriggerExpressionKind kind) {
        return switch (name) {
            case "PlayerEnter" -> TriggerEventKind.PLAYER_ENTER;
            case "PlayerExit" -> TriggerEventKind.PLAYER_EXIT;
            case "PlayerInside" -> TriggerEventKind.PLAYER_INSIDE;
            case "PlayerStay" -> TriggerEventKind.PLAYER_STAY;
            default -> kind == TriggerExpressionKind.PREDICATE
                    ? TriggerEventKind.PREDICATE_TRUE
                    : TriggerEventKind.VANILLA_GAMEPLAY;
        };
    }

    public static Map<String, String> normalizeArguments(String name, Map<String, String> raw) {
        Map<String, String> values = new LinkedHashMap<>(raw);
        move(values, "arg0", firstKey(name));
        move(values, "arg1", secondKey(name));
        return Map.copyOf(values);
    }

    private static String firstKey(String name) {
        Optional<String> exact = exactFirstKey(name);
        return exact.orElseGet(() -> patternedFirstKey(name));
    }

    private static Optional<String> exactFirstKey(String name) {
        if (name.equals("TimeBetween")) {
            return Optional.of("start");
        }
        if (name.equals("Wearing")) {
            return Optional.of("slot");
        }
        if (name.equals("GameModeChanged")) {
            return Optional.of("to");
        }
        return Optional.empty();
    }

    private static String patternedFirstKey(String name) {
        if (name.startsWith("Equipment")) {
            return "slot";
        }
        if (name.startsWith("Health") || name.startsWith("Food") || name.startsWith("Time")) {
            return "value";
        }
        if (name.startsWith("Day")) {
            return "day";
        }
        if (name.startsWith("ExperienceLevel")) {
            return "level";
        }
        if (name.startsWith("WeatherChanged")) {
            return "to";
        }
        return "target";
    }

    private static String secondKey(String name) {
        if (name.equals("TimeBetween")) {
            return "end";
        }
        if (name.equals("Wearing") || name.startsWith("Equipment")) {
            return "target";
        }
        if (name.startsWith("ItemCount")) {
            return "count";
        }
        if (name.startsWith("EffectAmplifier")) {
            return "amplifier";
        }
        if (name.startsWith("EffectDuration")) {
            return "ticks";
        }
        return "arg1";
    }

    private static void move(Map<String, String> values, String from, String to) {
        String value = values.remove(from);
        if (value != null && !values.containsKey(to)) {
            values.put(to, value);
        }
    }
}
