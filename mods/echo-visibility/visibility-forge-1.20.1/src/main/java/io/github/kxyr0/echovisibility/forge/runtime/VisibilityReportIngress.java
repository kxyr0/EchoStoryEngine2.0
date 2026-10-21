package io.github.kxyr0.echovisibility.forge.runtime;

import io.github.kxyr0.echovisibility.api.diagnostic.VisibilityDiagnostic;
import io.github.kxyr0.echovisibility.api.diagnostic.VisibilityDiagnosticCode;
import io.github.kxyr0.echovisibility.api.report.VisibilityReport;
import io.github.kxyr0.echovisibility.core.report.VisibilityEventIds;
import io.github.kxyr0.echovisibility.core.report.VisibilityReportValidation;
import io.github.kxyr0.echovisibility.core.report.VisibilityReportValidator;
import io.github.kxyr0.echovisibility.forge.api.lifecycle.VisibilityLifecycleEvent;
import io.github.kxyr0.echovisibility.forge.network.ClientboundReportAckPacket;
import io.github.kxyr0.echovisibility.forge.network.EchoVisibilityNetwork;
import io.github.kxyr0.echovisibility.forge.network.VisibilityReportNetworkEvent;
import io.github.kxyr0.echovisibility.forge.service.VisibilityServerState;
import io.github.kxyr0.echovisibility.forge.service.VisibilityWatchViews;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;

public final class VisibilityReportIngress {
    private final VisibilityServerState state;
    private final VisibilityReportValidator validator = new VisibilityReportValidator();

    public VisibilityReportIngress(VisibilityServerState state) {
        this.state = state;
    }

    public void accept(VisibilityReportNetworkEvent event) {
        ServerPlayer player = event.player();
        VisibilityReport report = event.report();
        var playerRef = VisibilityServerState.player(player);
        var armed = state.arms().find(playerRef, report.watchId());
        if (armed.isEmpty()) {
            reject(player, report, VisibilityDiagnosticCode.VISIBILITY_WATCH_NOT_ARMED,
                    "Report does not belong to an armed watch.");
            return;
        }
        if (state.resolve(player, armed.orElseThrow()).status()
                != io.github.kxyr0.echovisibility.forge.api.VisibilityTargetResolution.Status.RESOLVED) {
            reject(player, report, VisibilityDiagnosticCode.VISIBILITY_TARGET_NOT_FOUND,
                    "Visibility target is no longer available.");
            return;
        }
        VisibilityReportValidation validation = validator.validate(armed.orElseThrow(), report);
        if (validation.status() != VisibilityReportValidation.Status.ACCEPTED) {
            validation.diagnostic().ifPresent(state.diagnostics()::add);
            acknowledge(player, report, false, validation.status().name());
            return;
        }
        if (report.concealmentLeaseId().isPresent() && state.createLease(playerRef, report).isEmpty()) {
            reject(player, report, VisibilityDiagnosticCode.VISIBILITY_CONCEALMENT_REJECTED,
                    "Concealment lease id is duplicated or invalid.");
            return;
        }
        armed.orElseThrow().accept(report, state.server().getTickCount());
        state.markDirty();
        acknowledge(player, report, true, "ACCEPTED");
        var eventId = VisibilityEventIds.from(playerRef, report);
        if (state.firstEvent(eventId)) {
            MinecraftForge.EVENT_BUS.post(new VisibilityLifecycleEvent(player, eventId, report,
                    VisibilityWatchViews.from(armed.orElseThrow().snapshot())));
        }
    }

    private void reject(
            ServerPlayer player,
            VisibilityReport report,
            VisibilityDiagnosticCode code,
            String message) {
        VisibilityDiagnostic diagnostic = VisibilityDiagnostic.of(code, message);
        state.diagnostics().add(diagnostic);
        acknowledge(player, report, false, code.name());
    }

    private static void acknowledge(
            ServerPlayer player,
            VisibilityReport report,
            boolean accepted,
            String message) {
        EchoVisibilityNetwork.send(player, new ClientboundReportAckPacket(report.watchId(),
                report.generation(), report.reportSequence(), accepted, message));
    }
}
