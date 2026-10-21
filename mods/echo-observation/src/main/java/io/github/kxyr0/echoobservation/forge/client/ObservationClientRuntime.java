package io.github.kxyr0.echoobservation.forge.client;

import io.github.kxyr0.echoconcentration.api.ConcentrationServiceBridge;
import io.github.kxyr0.echoobservation.api.client.ObservationAuthoringReloadEvent;
import io.github.kxyr0.echoobservation.api.client.ObservationClientMetrics;
import io.github.kxyr0.echoobservation.api.client.ObservationClientScreenPoint;
import io.github.kxyr0.echoobservation.api.client.ObservationClientView;
import io.github.kxyr0.echoobservation.api.identity.ObservationId;
import io.github.kxyr0.echoobservation.api.model.ObservationDefinition;
import io.github.kxyr0.echoobservation.api.model.ObservationAnchor;
import io.github.kxyr0.echoobservation.core.catalog.ObservationCatalogSnapshot;
import io.github.kxyr0.echoobservation.forge.client.catalog.ObservationSpatialIndex;
import io.github.kxyr0.echoobservation.forge.client.input.ObservationFocusInput;
import io.github.kxyr0.echoobservation.forge.client.presentation.ObservationRingRenderer;
import io.github.kxyr0.echoobservation.forge.client.presentation.ObservationTextPresentation;
import io.github.kxyr0.echoobservation.forge.client.presentation.ProjectedObservation;
import io.github.kxyr0.echoobservation.forge.client.presentation.ObservationDistanceFade;
import io.github.kxyr0.echoobservation.forge.client.projection.ObservationRenderFrame;
import io.github.kxyr0.echoobservation.forge.client.visibility.ObservationOcclusion;
import io.github.kxyr0.echoobservation.forge.network.ObservationPackets;
import io.github.kxyr0.echovisibility.api.report.VisibilityCameraSnapshot;
import io.github.kxyr0.echovisibility.forge.api.debug.VisibilityClientDebugLookupEvent;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.common.MinecraftForge;

public final class ObservationClientRuntime implements ObservationClientView {
    private static final ObservationClientRuntime INSTANCE = new ObservationClientRuntime();
    private final ObservationSpatialIndex spatial = new ObservationSpatialIndex();
    private ObservationRenderFrame renderFrame;
    private final ObservationOcclusion occlusion = new ObservationOcclusion();
    private final ObservationRingRenderer rings = new ObservationRingRenderer();
    private final ObservationTextPresentation text = new ObservationTextPresentation();
    private final ObservationFocusInput focus = new ObservationFocusInput();
    private ObservationCatalogSnapshot catalog = ObservationCatalogSnapshot.empty();
    private Set<String> active = Set.of();
    private Set<String> disabled = Set.of();
    private Set<String> completed = Set.of();
    private Optional<ObservationDefinition> target = Optional.empty();
    private ObservationClientMetrics metrics = new ObservationClientMetrics(0, 0, 0, 0, 0, 0, 0, 0.0D, 0.0D);
    private final ArrayDeque<Long> packetTimes = new ArrayDeque<>();
    private int projectedCandidates;
    private int occlusionRaycasts;
    private long sequence;
    private int clientTick;

    private ObservationClientRuntime() {
    }

    public static ObservationClientRuntime instance() { return INSTANCE; }
    public void capture(ObservationRenderFrame value) { renderFrame = value; }

    public void accept(ObservationPackets.ClientboundSnapshot packet) {
        recordPacket();
        if (packet.sequence() <= sequence) return;
        sequence = packet.sequence();
        catalog = new ObservationCatalogSnapshot(packet.catalogRevision(), packet.resources());
        active = packet.activeResources();
        disabled = packet.disabledObservations();
        completed = packet.completedObservations();
        spatial.rebuild(catalog, active, disabled, completed);
        target = target.filter(value -> catalog.observation(value.id()).isPresent()
                && !completed.contains(value.id().toString()));
    }

    public void accept(ObservationPackets.ClientboundFocus packet) {
        recordPacket();
        focus.accept(packet, clientTick);
    }

    public void accept(ObservationPackets.ClientboundText packet) {
        recordPacket();
        text.show(packet.observationId(), packet.text(), packet.expiresAtMillis());
    }

    public void accept(ObservationPackets.ClientboundReload packet) {
        recordPacket();
        try {
            MinecraftForge.EVENT_BUS.post(new ObservationAuthoringReloadEvent(
                    ObservationId.parse(packet.observationId()), packet.successful(),
                    packet.catalogRevision(), packet.message()));
        } catch (IllegalArgumentException ignored) {
            // A malformed acknowledgement cannot target an authored declaration.
        }
    }

    public void tick() {
        clientTick++;
        boolean concentration = ConcentrationServiceBridge.lookup()
                .map(service -> service.snapshot().active()).orElse(false);
        focus.tick(target, concentration, clientTick);
        if (!concentration) target = Optional.empty();
    }

    public void render(GuiGraphics graphics) {
        text.render(graphics);
        boolean concentration = ConcentrationServiceBridge.lookup()
                .map(service -> service.snapshot().active()).orElse(false);
        Minecraft minecraft = Minecraft.getInstance();
        if (!concentration || minecraft.player == null || minecraft.level == null
                || minecraft.screen != null || renderFrame == null) return;
        Optional<VisibilityCameraSnapshot> camera = camera();
        if (camera.isEmpty()) {
            metrics = new ObservationClientMetrics(catalog.observationCount(), spatial.size(),
                    0, 0, 0, 0, packetsPerSecond(), 0.0D, 0.0D);
            return;
        }
        List<ObservationDefinition> candidates = spatial.nearby(
                minecraft.level.dimension().location().toString(), camera.orElseThrow().position().x(),
                camera.orElseThrow().position().z());
        projectedCandidates = 0;
        occlusionRaycasts = 0;
        List<ProjectedObservation> visible = visible(candidates, camera.orElseThrow());
        Optional<ProjectedObservation> selected = visible.stream()
                .filter(value -> value.centerDistance() <= value.physicalRadius())
                .min(Comparator.comparingDouble(ProjectedObservation::centerDistance)
                        .thenComparingDouble(value -> value.point().depth())
                        .thenComparing(value -> value.definition().id()));
        target = selected.map(ProjectedObservation::definition);
        double x = selected.map(value -> value.point().framebufferX()).orElse(0.0D);
        double y = selected.map(value -> value.point().framebufferY()).orElse(0.0D);
        metrics = new ObservationClientMetrics(catalog.observationCount(), spatial.size(), candidates.size(),
                projectedCandidates, visible.size(), occlusionRaycasts, packetsPerSecond(), x, y);
        rings.render(graphics, visible, selected.orElse(null),
                focus.progress(clientTick + minecraft.getFrameTime()));
    }

    private List<ProjectedObservation> visible(List<ObservationDefinition> candidates,
            VisibilityCameraSnapshot camera) {
        List<ProjectedObservation> result = new ArrayList<>();
        double centerX = renderFrame.framebufferWidth() / 2.0D;
        double centerY = renderFrame.framebufferHeight() / 2.0D;
        for (ObservationDefinition definition : candidates) {
            var anchor = definition.anchor().orElseThrow();
            double distance = distance(camera, anchor.worldX(), anchor.worldY(), anchor.worldZ());
            if (distance > definition.distance()) continue;
            var point = renderFrame.project(anchor);
            if (point.isEmpty()) continue;
            projectedCandidates++;
            occlusionRaycasts++;
            if (!occlusion.visible(anchor, camera)) continue;
            double radius = definition.screenRadius() * renderFrame.framebufferHeight() / 1080.0D;
            double centerDistance = Math.hypot(point.orElseThrow().framebufferX() - centerX,
                    point.orElseThrow().framebufferY() - centerY);
            double opacity = ObservationDistanceFade.opacity(distance, definition.distance());
            result.add(new ProjectedObservation(
                    definition, point.orElseThrow(), radius, centerDistance, opacity));
        }
        return result;
    }

    private double distance(VisibilityCameraSnapshot camera, double x, double y, double z) {
        return Math.sqrt(Math.pow(x - camera.position().x(), 2.0D)
                + Math.pow(y - camera.position().y(), 2.0D)
                + Math.pow(z - camera.position().z(), 2.0D));
    }

    private Optional<VisibilityCameraSnapshot> camera() {
        VisibilityClientDebugLookupEvent event = new VisibilityClientDebugLookupEvent();
        MinecraftForge.EVENT_BUS.post(event);
        return event.view().flatMap(view -> view.camera());
    }

    public void clear() {
        catalog = ObservationCatalogSnapshot.empty(); active = Set.of(); disabled = Set.of(); completed = Set.of();
        spatial.rebuild(catalog, active, disabled, completed); target = Optional.empty(); focus.clear(); text.clear();
        sequence = 0L; renderFrame = null;
        packetTimes.clear();
    }

    @Override public ObservationCatalogSnapshot catalog() { return catalog; }
    @Override public Set<String> activeResourceIds() { return active; }
    @Override public Set<String> completedObservationIds() { return completed; }
    @Override public Optional<ObservationId> targeted() { return target.map(ObservationDefinition::id); }
    @Override public Optional<ObservationClientScreenPoint> project(ObservationAnchor anchor) {
        return renderFrame == null ? Optional.empty() : renderFrame.project(anchor)
                .map(value -> new ObservationClientScreenPoint(
                        value.framebufferX(), value.framebufferY(), value.depth()));
    }
    @Override public ObservationClientMetrics metrics() { return metrics; }

    public void recordPacket() {
        packetTimes.addLast(System.currentTimeMillis());
        packetsPerSecond();
    }

    private int packetsPerSecond() {
        long cutoff = System.currentTimeMillis() - 1000L;
        while (!packetTimes.isEmpty() && packetTimes.peekFirst() < cutoff) packetTimes.removeFirst();
        return packetTimes.size();
    }
}
