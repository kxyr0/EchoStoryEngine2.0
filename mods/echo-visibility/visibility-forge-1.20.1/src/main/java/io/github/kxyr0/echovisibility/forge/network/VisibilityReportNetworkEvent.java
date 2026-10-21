package io.github.kxyr0.echovisibility.forge.network;

import io.github.kxyr0.echovisibility.api.report.VisibilityReport;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

public final class VisibilityReportNetworkEvent extends Event {
    private final ServerPlayer player;
    private final VisibilityReport report;

    public VisibilityReportNetworkEvent(ServerPlayer player, VisibilityReport report) {
        this.player = Objects.requireNonNull(player, "player");
        this.report = Objects.requireNonNull(report, "report");
    }

    public ServerPlayer player() {
        return player;
    }

    public VisibilityReport report() {
        return report;
    }
}
