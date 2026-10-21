package io.github.kxyr0.echovisibility.forge.api.lifecycle;

import io.github.kxyr0.echovisibility.api.identity.VisibilityEventId;
import io.github.kxyr0.echovisibility.api.report.VisibilityReport;
import io.github.kxyr0.echovisibility.forge.api.VisibilityWatchView;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

public final class VisibilityLifecycleEvent extends Event {
    private final ServerPlayer player;
    private final VisibilityEventId eventId;
    private final VisibilityReport report;
    private final VisibilityWatchView watch;

    public VisibilityLifecycleEvent(
            ServerPlayer player,
            VisibilityEventId eventId,
            VisibilityReport report,
            VisibilityWatchView watch) {
        this.player = Objects.requireNonNull(player, "player");
        this.eventId = Objects.requireNonNull(eventId, "eventId");
        this.report = Objects.requireNonNull(report, "report");
        this.watch = Objects.requireNonNull(watch, "watch");
    }

    public ServerPlayer player() {
        return player;
    }

    public VisibilityEventId eventId() {
        return eventId;
    }

    public VisibilityReport report() {
        return report;
    }

    public VisibilityWatchView watch() {
        return watch;
    }
}
