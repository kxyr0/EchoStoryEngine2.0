package io.github.kxyr0.echoscene.forge.api;

import io.github.kxyr0.echoscene.api.command.SceneCommandRequest;
import io.github.kxyr0.echoscene.api.command.SceneCommandStart;
import io.github.kxyr0.echoscene.forge.command.MovementCommandAdapter.LocationCatalog;
import io.github.kxyr0.echoscene.forge.command.SceneCommandHub.Completion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/** Narrow synchronous extension boundary; the Scene remains owner of leases and receipts. */
public final class SceneExternalCommandEvent extends Event {
    private final SceneCommandRequest request;
    private final ServerPlayer player;
    private final LocationCatalog locations;
    private final Completion completion;
    private SceneCommandStart result;

    public SceneExternalCommandEvent(SceneCommandRequest request, ServerPlayer player,
            LocationCatalog locations, Completion completion) {
        this.request = request;
        this.player = player;
        this.locations = locations;
        this.completion = completion;
    }
    public SceneCommandRequest request() { return request; }
    public ServerPlayer player() { return player; }
    public LocationCatalog locations() { return locations; }
    public Completion completion() { return completion; }
    public java.util.Optional<SceneCommandStart> result() { return java.util.Optional.ofNullable(result); }
    public void provide(SceneCommandStart value) {
        if (result != null) throw new IllegalStateException("Duplicate Scene command provider");
        result = java.util.Objects.requireNonNull(value);
    }
}
