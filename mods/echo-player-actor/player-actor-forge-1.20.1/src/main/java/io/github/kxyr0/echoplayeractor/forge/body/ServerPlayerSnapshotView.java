package io.github.kxyr0.echoplayeractor.forge.body;

import io.github.kxyr0.echoplayeractor.api.pose.PlayerActorSnapshot;
import io.github.kxyr0.echoplayeractor.forge.api.view.PlayerActorSnapshotView;
import java.util.Objects;

public record ServerPlayerSnapshotView(PlayerActorSnapshot snapshot) implements PlayerActorSnapshotView {
    public ServerPlayerSnapshotView {
        snapshot = Objects.requireNonNull(snapshot, "snapshot");
    }
}
