package io.github.kxyr0.echotrigger.api.subscription;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

public record TriggerEventSelector(
        String providerId,
        String eventKind,
        String scenarioId,
        Optional<String> zoneId,
        Optional<String> slotId,
        OptionalLong sleepCount,
        OptionalLong sleepCountAtLeast,
        String fingerprint) {
    public TriggerEventSelector {
        providerId = requireId(providerId, "providerId");
        eventKind = requireText(eventKind, "eventKind").toUpperCase(java.util.Locale.ROOT);
        scenarioId = requireId(scenarioId, "scenarioId");
        zoneId = Objects.requireNonNull(zoneId, "zoneId").map(value -> requireId(value, "zoneId"));
        slotId = Objects.requireNonNull(slotId, "slotId").map(value -> requireId(value, "slotId"));
        sleepCount = Objects.requireNonNull(sleepCount, "sleepCount");
        sleepCountAtLeast = Objects.requireNonNull(sleepCountAtLeast, "sleepCountAtLeast");
        if (sleepCount.isPresent() && sleepCount.orElseThrow() <= 0L) {
            throw new IllegalArgumentException("sleepCount must be positive.");
        }
        if (sleepCountAtLeast.isPresent() && sleepCountAtLeast.orElseThrow() <= 0L) {
            throw new IllegalArgumentException("sleepCountAtLeast must be positive.");
        }
        if (sleepCount.isPresent() && sleepCountAtLeast.isPresent()) {
            throw new IllegalArgumentException("sleepCount conflicts with sleepCountAtLeast.");
        }
        fingerprint = requireText(fingerprint, "fingerprint");
    }

    public static TriggerEventSelector of(String providerId, String eventKind, String scenarioId) {
        return of(providerId, eventKind, scenarioId, OptionalLong.empty(), OptionalLong.empty());
    }

    public static TriggerEventSelector of(
            String providerId,
            String eventKind,
            String scenarioId,
            OptionalLong sleepCount,
            OptionalLong sleepCountAtLeast) {
        String key = requireId(providerId, "providerId") + "|" + requireText(eventKind, "eventKind")
                + "|" + requireId(scenarioId, "scenarioId") + constraintKey(sleepCount, sleepCountAtLeast);
        return new TriggerEventSelector(providerId, eventKind, scenarioId,
                Optional.empty(), Optional.empty(), sleepCount, sleepCountAtLeast,
                Integer.toUnsignedString(key.hashCode(), 16));
    }

    public static TriggerEventSelector seating(
            String providerId,
            String eventKind,
            String zoneId,
            Optional<String> slotId) {
        String zone = requireId(zoneId, "zoneId");
        Optional<String> slot = Objects.requireNonNull(slotId, "slotId")
                .map(value -> requireId(value, "slotId"));
        String key = requireId(providerId, "providerId") + "|"
                + requireText(eventKind, "eventKind") + "|" + zone
                + slot.map(value -> "|slot=" + value).orElse("");
        return new TriggerEventSelector(providerId, eventKind, zone,
                Optional.of(zone), slot, OptionalLong.empty(), OptionalLong.empty(),
                Integer.toUnsignedString(key.hashCode(), 16));
    }

    public static TriggerEventSelector seatingAt(String providerId, String eventKind, String slotId) {
        String slot = requireId(slotId, "slotId");
        String key = requireId(providerId, "providerId") + "|"
                + requireText(eventKind, "eventKind") + "|slot=" + slot;
        return new TriggerEventSelector(providerId, eventKind, slot,
                Optional.empty(), Optional.of(slot), OptionalLong.empty(), OptionalLong.empty(),
                Integer.toUnsignedString(key.hashCode(), 16));
    }

    public String exactKey() {
        if (zoneId.isPresent()) {
            return providerId + "|" + eventKind + "|" + zoneId.orElseThrow()
                    + slotId.map(value -> "|slot=" + value).orElse("");
        }
        if (slotId.isPresent()) {
            return providerId + "|" + eventKind + "|slot=" + slotId.orElseThrow();
        }
        return providerId + "|" + eventKind + "|" + scenarioId + constraintKey(sleepCount, sleepCountAtLeast);
    }

    public boolean acceptsSleepCount(long count) {
        if (sleepCount.isPresent()) {
            return count == sleepCount.orElseThrow();
        }
        return sleepCountAtLeast.isEmpty() || count >= sleepCountAtLeast.orElseThrow();
    }

    public boolean sameEvent(String provider, String kind, String scenario) {
        return providerId.equals(provider)
                && eventKind.equals(requireText(kind, "kind").toUpperCase(java.util.Locale.ROOT))
                && scenarioId.equals(scenario);
    }

    public boolean sameSeatingEvent(String provider, String kind, String zone, Optional<String> slot) {
        Optional<String> checkedSlot = Objects.requireNonNull(slot, "slot")
                .map(value -> requireId(value, "slotId"));
        return providerId.equals(provider)
                && eventKind.equals(requireText(kind, "kind").toUpperCase(java.util.Locale.ROOT))
                && (zoneId.isEmpty() || zoneId.filter(requireId(zone, "zoneId")::equals).isPresent())
                && slotId.equals(checkedSlot);
    }

    private static String constraintKey(OptionalLong sleepCount, OptionalLong sleepCountAtLeast) {
        if (sleepCount.isPresent()) {
            return "|sleepCount=" + sleepCount.orElseThrow();
        }
        if (sleepCountAtLeast.isPresent()) {
            return "|sleepCountAtLeast=" + sleepCountAtLeast.orElseThrow();
        }
        return "";
    }

    private static String requireId(String value, String name) {
        String checked = requireText(value, name);
        if (!checked.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException(name + " must be namespace:path.");
        }
        return checked;
    }

    private static String requireText(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return checked;
    }
}
