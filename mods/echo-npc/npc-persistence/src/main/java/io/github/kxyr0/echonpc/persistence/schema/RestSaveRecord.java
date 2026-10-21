package io.github.kxyr0.echonpc.persistence.schema;

import java.util.Objects;

/**
 * Echo NPC durable rest attachment DTO embedded in Actor Save Schema V2.
 */
public record RestSaveRecord(
        String providerId,
        String bedWorldId,
        int bedX,
        int bedY,
        int bedZ,
        String expectedBlockId,
        int canonicalBedX,
        int canonicalBedY,
        int canonicalBedZ,
        double anchorX,
        double anchorY,
        double anchorZ,
        float sleepYaw,
        String pose,
        long attachmentRevision,
        String providerState,
        boolean occupied,
        String commandId
) {
    public RestSaveRecord {
        providerId = text(providerId, "providerId");
        bedWorldId = text(bedWorldId, "bedWorldId");
        expectedBlockId = text(expectedBlockId, "expectedBlockId");
        pose = text(pose, "pose");
        providerState = Objects.requireNonNull(providerState, "providerState");
        commandId = text(commandId, "commandId");
        if (!Double.isFinite(anchorX) || !Double.isFinite(anchorY) || !Double.isFinite(anchorZ)) {
            throw new IllegalArgumentException("sleep anchor must be finite.");
        }
        if (!Float.isFinite(sleepYaw) || attachmentRevision < 0L) {
            throw new IllegalArgumentException("sleepYaw must be finite and revision must not be negative.");
        }
    }

    private static String text(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty() || checked.length() > 256) {
            throw new IllegalArgumentException(name + " must be non-empty and bounded.");
        }
        return checked;
    }
}
