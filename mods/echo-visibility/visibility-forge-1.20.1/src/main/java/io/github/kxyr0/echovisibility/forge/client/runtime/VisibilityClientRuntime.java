package io.github.kxyr0.echovisibility.forge.client.runtime;

import io.github.kxyr0.echovisibility.api.debug.VisibilityDebugSnapshot;
import io.github.kxyr0.echovisibility.api.debug.VisibilityPerformanceSnapshot;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchId;
import io.github.kxyr0.echovisibility.api.policy.VisibilityEvaluationRate;
import io.github.kxyr0.echovisibility.api.report.VisibilityCameraSnapshot;
import io.github.kxyr0.echovisibility.forge.api.debug.VisibilityClientDebugView;
import io.github.kxyr0.echovisibility.forge.client.camera.ActiveCameraSnapshotCapture;
import io.github.kxyr0.echovisibility.forge.client.concealment.ClientConcealmentState;
import io.github.kxyr0.echovisibility.forge.network.ClientboundArmPacket;
import io.github.kxyr0.echovisibility.forge.network.ClientboundConcealmentPacket;
import io.github.kxyr0.echovisibility.forge.network.ClientboundDisarmPacket;
import io.github.kxyr0.echovisibility.forge.network.ClientboundReportAckPacket;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Camera;

public final class VisibilityClientRuntime implements VisibilityClientDebugView {
    private static final VisibilityClientRuntime INSTANCE = new VisibilityClientRuntime();
    private final Map<VisibilityWatchId, ClientArmedWatch> watches = new LinkedHashMap<>();
    private final ActiveCameraSnapshotCapture cameraCapture = new ActiveCameraSnapshotCapture();
    private final ClientConcealmentState concealment = new ClientConcealmentState();
    private final VisibilityPerformanceCounters performance = new VisibilityPerformanceCounters();
    private final VisibilityClientEvaluator evaluator =
            new VisibilityClientEvaluator(cameraCapture, concealment, performance);
    private Optional<VisibilityCameraSnapshot> camera = Optional.empty();
    private float partialTick;
    private long renderedFrame;
    private long clientTick;

    private VisibilityClientRuntime() {
    }

    public static VisibilityClientRuntime instance() {
        return INSTANCE;
    }

    public void arm(ClientboundArmPacket packet) {
        ClientArmedWatch current = watches.get(packet.definition().id());
        if (current != null && current.generation().value() > packet.generation().value()) {
            return;
        }
        if (current != null) {
            concealment.disarm(current);
        }
        watches.put(packet.definition().id(), new ClientArmedWatch(packet.definition(),
                packet.generation(), packet.connectionGeneration(), packet.stableBounds(),
                packet.clientEntityId()));
    }

    public void disarm(ClientboundDisarmPacket packet) {
        ClientArmedWatch current = watches.get(packet.watchId());
        if (current != null && current.generation().equals(packet.generation())) {
            watches.remove(packet.watchId());
            concealment.disarm(current);
        }
    }

    public void concealment(ClientboundConcealmentPacket packet) {
        concealment.apply(packet);
    }

    public void acknowledge(ClientboundReportAckPacket packet) {
        ClientArmedWatch watch = watches.get(packet.watchId());
        if (watch != null && watch.generation().equals(packet.generation())) {
            watch.acknowledge(packet.reportSequence(), packet.accepted(), packet.message());
        }
    }

    public void render(Camera activeCamera, float framePartialTick) {
        renderedFrame++;
        performance.frame();
        partialTick = framePartialTick;
        camera = cameraCapture.capture(activeCamera, framePartialTick);
        camera.ifPresent(snapshot -> evaluate(VisibilityEvaluationRate.EVERY_RENDER_FRAME, snapshot));
    }

    public void tick() {
        clientTick++;
        concealment.tick(clientTick);
        camera.ifPresent(snapshot -> evaluate(VisibilityEvaluationRate.EVERY_CLIENT_TICK, snapshot));
    }

    public void clear() {
        for (ClientArmedWatch watch : watches.values()) {
            concealment.disarm(watch);
        }
        watches.clear();
        camera = Optional.empty();
    }

    public boolean hidesNpc(String npcId) {
        return concealment.hidesNpc(npcId);
    }

    @Override
    public Optional<VisibilityCameraSnapshot> camera() {
        return camera;
    }

    @Override
    public Optional<VisibilityDebugSnapshot> watch(VisibilityWatchId watchId) {
        ClientArmedWatch watch = watches.get(watchId);
        return watch == null ? Optional.empty() : watch.debug();
    }

    @Override
    public List<VisibilityDebugSnapshot> activeWatches() {
        return watches.values().stream().flatMap(value -> value.debug().stream()).toList();
    }

    @Override
    public VisibilityPerformanceSnapshot performance() {
        return performance.snapshot(watches.size());
    }

    private void evaluate(VisibilityEvaluationRate rate, VisibilityCameraSnapshot snapshot) {
        for (ClientArmedWatch watch : List.copyOf(watches.values())) {
            if (watch.definition().evaluationRate() == rate) {
                evaluator.evaluate(watch, snapshot, partialTick, renderedFrame, clientTick);
            }
        }
    }
}
