package io.github.kxyr0.echonavigation.forge.persistence;

import io.github.kxyr0.echonavigation.api.movement.NavigationSnapshot;

public final class NavigationSavedDataCodec {
    public String encode(NavigationSnapshot snapshot) {
        return String.join("|",
                snapshot.moveId().value(),
                snapshot.commandId().value(),
                snapshot.executionId().value(),
                snapshot.ownerId().value(),
                snapshot.subjectId().value(),
                snapshot.destination().dimensionId(),
                Integer.toString(snapshot.destination().blockX()),
                Integer.toString(snapshot.destination().blockY()),
                Integer.toString(snapshot.destination().blockZ()),
                snapshot.status().name(),
                snapshot.outcome().map(Enum::name).orElse("-"),
                snapshot.failure().map(Enum::name).orElse("-"),
                snapshot.backendId().value(),
                Long.toString(snapshot.settingsRevision()),
                Double.toString(snapshot.progress().currentPosition().x()),
                Double.toString(snapshot.progress().currentPosition().y()),
                Double.toString(snapshot.progress().currentPosition().z()),
                Long.toString(snapshot.progress().serverTick()),
                Integer.toString(snapshot.progress().repathCount()));
    }
}
