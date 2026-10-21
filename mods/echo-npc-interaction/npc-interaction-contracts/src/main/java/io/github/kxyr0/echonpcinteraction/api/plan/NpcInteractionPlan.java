package io.github.kxyr0.echonpcinteraction.api.plan;

import io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionProviderId;
import io.github.kxyr0.echonpcinteraction.api.target.NpcInteractionTargetSnapshot;
import java.util.List;
import java.util.Objects;

public record NpcInteractionPlan(
        NpcInteractionProviderId providerId,
        NpcInteractionTargetSnapshot targetSnapshot,
        List<NpcInteractionApproach> approaches,
        String executionPolicy,
        String verificationPolicy,
        long planRevision) {
    public NpcInteractionPlan {
        providerId = Objects.requireNonNull(providerId, "providerId");
        targetSnapshot = Objects.requireNonNull(targetSnapshot, "targetSnapshot");
        approaches = List.copyOf(Objects.requireNonNull(approaches, "approaches"));
        executionPolicy = requireText(executionPolicy, "executionPolicy");
        verificationPolicy = requireText(verificationPolicy, "verificationPolicy");
        if (approaches.isEmpty() || planRevision < 1L) {
            throw new IllegalArgumentException("plan must have approaches and positive revision.");
        }
    }

    private static String requireText(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must be non-empty.");
        }
        return checked;
    }
}
