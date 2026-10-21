package io.github.kxyr0.echonavigation.core.movement;

import io.github.kxyr0.echonavigation.api.backend.NavigationSettingsSnapshot;
import io.github.kxyr0.echonavigation.api.movement.NavigationFailure;
import io.github.kxyr0.echonavigation.api.movement.NavigationRequest;
import java.util.Optional;

public final class NavigationRequestValidator {
    public Optional<NavigationFailure> validate(NavigationRequest request, NavigationSettingsSnapshot settings) {
        if (!request.backendId().equals(settings.backendId())) {
            return Optional.of(NavigationFailure.NAVIGATION_BACKEND_CHANGE_REJECTED);
        }
        if (request.settingsRevision() != settings.revision()) {
            return Optional.of(NavigationFailure.NAVIGATION_BACKEND_SETTINGS_INVALID);
        }
        if (!request.destination().dimensionId().contains(":")) {
            return Optional.of(NavigationFailure.NPC_MOVE_DESTINATION_INVALID);
        }
        return Optional.empty();
    }
}
