package io.github.kxyr0.echothoughtspace.forge.client;

import io.github.kxyr0.echothoughtspace.api.ThoughtNodeId;
import io.github.kxyr0.echothoughtspace.api.ThoughtNodeProvider;
import io.github.kxyr0.echothoughtspace.api.ThoughtCanvasPosition;
import io.github.kxyr0.echothoughtspace.core.canvas.CanvasCamera;
import io.github.kxyr0.echothoughtspace.core.canvas.CanvasPoint;
import io.github.kxyr0.echothoughtspace.core.canvas.CanvasViewport;
import io.github.kxyr0.echothoughtspace.core.layout.ThoughtConnectionTrace;
import io.github.kxyr0.echothoughtspace.core.combination.UnlockedThoughtProvider;
import io.github.kxyr0.echothoughtspace.core.lifecycle.ThoughtSpaceState;
import io.github.kxyr0.echothoughtspace.core.lifecycle.ThoughtSpaceTransition;
import io.github.kxyr0.echothoughtspace.core.model.ThoughtCanvasNode;
import io.github.kxyr0.echothoughtspace.core.model.ThoughtNodeRepository;
import io.github.kxyr0.echothoughtspace.forge.config.ThoughtSpaceClientConfig;
import io.github.kxyr0.echothoughtspace.forge.client.style.ThoughtSpaceStyleConfig;
import io.github.kxyr0.echothoughtspace.forge.client.style.ThoughtSpaceStyleSource;
import java.lang.reflect.Method;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;

public final class ThoughtSpaceRuntime {
    private final ThoughtSpaceTransition transition = new ThoughtSpaceTransition(
            ThoughtSpaceClientConfig.fadeMillis());
    private final ThoughtSpaceInputCapture capture = new ThoughtSpaceInputCapture();
    private final ThoughtNodeProviderRegistry providers = new ThoughtNodeProviderRegistry();
    private final ThoughtNodeRepository repository = new ThoughtNodeRepository();
    private final UnlockedThoughtProvider unlockedThoughts = new UnlockedThoughtProvider();
    private final CanvasCamera camera = new CanvasCamera();
    private final ThoughtSpaceStyleSource styles = new ThoughtSpaceStyleSource();
    private final Map<ThoughtNodeId, ThoughtCanvasPosition> derivedPositions = new LinkedHashMap<>();
    private final Map<ThoughtNodeId, ThoughtConnectionTrace> connectionTraces = new LinkedHashMap<>();
    private List<ThoughtNodeProvider> providerView = List.of();
    private List<ThoughtCanvasNode> nodes = List.of();
    private ThoughtSpaceScreen screen;
    private long lastFrameNanos;
    private long appliedStyleRevision = Long.MIN_VALUE;
    private boolean debug;

    public void toggleEdge() {
        long now = System.nanoTime();
        if (transition.state() == ThoughtSpaceState.CLOSED) open(now);
        else if (transition.state() == ThoughtSpaceState.OPEN) transition.close(now);
    }

    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (capture.active()) {
            styles.refresh();
            applyStyleLimits();
        }
        capture.maintain(minecraft);
        if (capture.active()) nodes = repository.refresh(providerView, derivedPositions);
        if (capture.active() && (minecraft.screen != screen || !ThoughtSpaceFeatureAccess.allowed())) reset();
    }

    public Frame frame(long nowNanos) {
        transition.update(nowNanos);
        double delta = lastFrameNanos == 0L ? 1.0D / 60.0D
                : Math.min(0.10D, Math.max(0.0D, (nowNanos - lastFrameNanos) / 1_000_000_000.0D));
        lastFrameNanos = nowNanos;
        double opacity = transition.opacity(nowNanos);
        if (transition.closeFrameComplete(nowNanos)) finishClose();
        return new Frame(opacity, delta);
    }

    public void reset() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == screen) minecraft.setScreen(null);
        screen = null;
        capture.release();
        transition.resetClosed();
        lastFrameNanos = 0L;
    }

    public void logout() {
        reset();
        unlockedThoughts.clear();
        derivedPositions.clear();
        connectionTraces.clear();
        providerView = List.of();
        nodes = List.of();
    }

    private void open(long nowNanos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!ThoughtSpaceFeatureAccess.allowed()) return;
        if (minecraft.level == null || minecraft.player == null || minecraft.screen != null || cutsceneActive()) return;
        styles.refresh();
        providers.discover();
        providerView = collectProviders();
        nodes = repository.refresh(providerView, derivedPositions);
        camera.reset(styles.current().zoomInitial());
        appliedStyleRevision = styles.revision();
        transition.open(nowNanos);
        capture.acquire(minecraft);
        screen = new ThoughtSpaceScreen(this);
        minecraft.setScreen(screen);
        lastFrameNanos = nowNanos;
    }


    private void finishClose() {
        Minecraft minecraft = Minecraft.getInstance();
        ThoughtSpaceScreen closing = screen;
        screen = null;
        if (minecraft.screen == closing) minecraft.setScreen(null);
        capture.release();
        transition.finishClosed();
        lastFrameNanos = 0L;
    }

    public void refreshForCommand() {
        styles.refresh();
        if (providerView.isEmpty()) { providers.discover(); providerView = collectProviders(); }
        nodes = repository.refresh(providerView, derivedPositions);
    }

    private List<ThoughtNodeProvider> collectProviders() {
        return Stream.concat(providers.providers().stream(),
                Stream.of(styles.declaredThoughts(), unlockedThoughts)).toList();
    }

    private static boolean cutsceneActive() {
        try {
            Class<?> type = Class.forName("io.github.kxyr0.echocutscene.forge.client.EchoCutsceneClient");
            Method method = type.getMethod("hudSuppressed");
            return Boolean.TRUE.equals(method.invoke(null));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    public void zoom(CanvasPoint pointer, CanvasViewport viewport, double wheelDelta) {
        if (!Double.isFinite(wheelDelta) || wheelDelta == 0.0D) return;
        ThoughtSpaceStyleConfig style = styles.current();
        double notches = Math.max(-16.0D, Math.min(16.0D, wheelDelta));
        double target = camera.zoom() * Math.pow(1.0D + style.zoomStep(), notches);
        target = Math.max(style.zoomMin(), Math.min(style.zoomMax(), target));
        camera.zoomAt(pointer, target, viewport);
    }

    public boolean canCombine(ThoughtNodeId first, ThoughtNodeId second) {
        return styles.combinations().find(first, second).isPresent();
    }

    public boolean combine(ThoughtNodeId first, ThoughtNodeId second) {
        var a = position(first); var b = position(second);
        return combine(first, second, List.of(new CanvasPoint(a.x(), a.y()), new CanvasPoint(b.x(), b.y())));
    }

    public boolean combine(ThoughtNodeId first, ThoughtNodeId second, List<CanvasPoint> path) {
        var recipe = styles.combinations().find(first, second);
        if (recipe.isEmpty()) return false;
        if (nodeExists(recipe.get().result()) && !unlockedThoughts.contains(recipe.get().result())) return false;
        if (unlockedThoughts.unlock(recipe.get())) {
            var trace = ThoughtConnectionTrace.split(path);
            connectionTraces.put(recipe.get().result(), trace);
            derivedPositions.put(recipe.get().result(), new ThoughtCanvasPosition(trace.result().x(), trace.result().y()));
            nodes = repository.refresh(providerView, derivedPositions);
        }
        return true;
    }

    private ThoughtCanvasPosition position(ThoughtNodeId id) {
        return nodes.stream().filter(node -> node.snapshot().id().equals(id))
                .findFirst().orElseThrow().position();
    }

    private boolean nodeExists(ThoughtNodeId id) {
        return nodes.stream().anyMatch(node -> node.snapshot().id().equals(id));
    }

    private void applyStyleLimits() {
        if (appliedStyleRevision == styles.revision()) return;
        ThoughtSpaceStyleConfig style = styles.current();
        camera.constrainZoom(style.zoomMin(), style.zoomMax());
        appliedStyleRevision = styles.revision();
    }

    public CanvasCamera camera() { return camera; }
    public ThoughtSpaceStyleConfig style() { return styles.current(); }
    public long styleRevision() { return styles.revision(); }
    public int styleFileCount() { return styles.fileCount(); }
    public List<io.github.kxyr0.echothoughtspace.core.combination.ThoughtCombinationRecipe> combinations() {
        return styles.combinations().recipes();
    }
    public List<io.github.kxyr0.echothoughtspace.core.combination.ThoughtCombinationRecipe> unlockedCombinations() { return combinations().stream().filter(recipe -> unlockedThoughts.contains(recipe.result())).toList(); }
    public int unlockedCombinationCount() { return unlockedThoughts.size(); }
    public List<ThoughtCanvasNode> nodes() { return nodes; }
    public Map<ThoughtNodeId, ThoughtConnectionTrace> connectionTraces() { return Map.copyOf(connectionTraces); }
    public ThoughtSpaceState state() { return transition.state(); }
    public boolean captureActive() { return capture.active(); }
    public int providerCount() { return providerView.size(); }
    public boolean debug() { return debug; }
    public void debug(boolean value) { debug = value; }
    public record Frame(double opacity, double deltaSeconds) { }
}
