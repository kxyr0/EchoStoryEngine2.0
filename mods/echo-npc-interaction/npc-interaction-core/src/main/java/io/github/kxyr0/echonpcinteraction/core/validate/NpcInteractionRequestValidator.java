package io.github.kxyr0.echonpcinteraction.core.validate;

import io.github.kxyr0.echonpcinteraction.api.request.NpcInteractionRequest;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionFailure;
import java.util.Optional;
import java.util.Set;

public final class NpcInteractionRequestValidator {
    private static final Set<String> FACES = Set.of("AUTO", "UP", "DOWN", "NORTH", "SOUTH", "WEST", "EAST");
    private static final Set<String> HANDS = Set.of("MAIN_HAND", "OFF_HAND");
    private static final Set<String> APPROACHES = Set.of("AUTO");

    public Optional<NpcInteractionFailure> validate(NpcInteractionRequest request) {
        if (!FACES.contains(request.face().toUpperCase(java.util.Locale.ROOT))) {
            return Optional.of(NpcInteractionFailure.NPC_INTERACTION_FACE_UNRESOLVED);
        }
        if (!HANDS.contains(request.hand().toUpperCase(java.util.Locale.ROOT))) {
            return Optional.of(NpcInteractionFailure.NPC_INTERACTION_REQUIRED_ITEM_MISSING);
        }
        if (!APPROACHES.contains(request.approach().toUpperCase(java.util.Locale.ROOT))) {
            return Optional.of(NpcInteractionFailure.NPC_INTERACTION_NO_APPROACH);
        }
        return Optional.empty();
    }
}
