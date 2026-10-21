package io.github.kxyr0.echoplayeractor.core.interaction;

import io.github.kxyr0.echonavigation.api.position.Vector3;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorInteractionRequest;
import java.util.Objects;

public final class PlayerInteractionPlanner {
    public PlayerInteractionPlan plan(
            PlayerActorInteractionRequest request,
            Vector3 eyePosition,
            boolean lineOfSight
    ) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(eyePosition, "eyePosition");
        Vector3 target = new Vector3(request.x() + 0.5D, request.y() + 0.5D, request.z() + 0.5D);
        double dx = target.x() - eyePosition.x();
        double dy = target.y() - eyePosition.y();
        double dz = target.z() - eyePosition.z();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        String receipt = request.commandId() + "@" + request.x() + "," + request.y() + "," + request.z();
        return new PlayerInteractionPlan(request, distance, distance <= request.maxDistance(), lineOfSight, receipt);
    }
}
