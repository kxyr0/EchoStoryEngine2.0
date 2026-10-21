package io.github.kxyr0.echovisibility.forge.api;

import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchId;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

public interface VisibilityQueryService {
    Optional<VisibilityWatchView> find(ServerPlayer player, VisibilityWatchId watchId);

    List<VisibilityWatchView> active(ServerPlayer player);

    boolean isVisible(ServerPlayer player, VisibilityWatchId watchId);

    boolean isOutOfView(ServerPlayer player, VisibilityWatchId watchId);
}
