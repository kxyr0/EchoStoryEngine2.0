package io.github.kxyr0.echomagic.forge.client.gesture;

import io.github.kxyr0.echomagic.api.gesture.MagicGestureSymbol;
import io.github.kxyr0.echomagic.core.gesture.MagicGestureMode;
import io.github.kxyr0.echomagic.core.gesture.MagicGesturePoint;
import io.github.kxyr0.echomagic.core.gesture.MagicGestureRecognizer;
import io.github.kxyr0.echomagic.forge.config.MagicGestureHudConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ViewportEvent;
import org.lwjgl.glfw.GLFW;

final class MagicGestureSession {
    static final int MAXIMUM_STROKES = 8;
    private static final int CAPTURE_POINT_LIMIT = 128;
    private static final double POINT_SPACING = 0.018D;
    private final MagicGestureMode mode;
    private final MagicGesturePlane plane;
    private final Optional<MagicGestureSymbol> drawingSchool;
    private final List<List<MagicGesturePoint>> strokes = new ArrayList<>();
    private final long startedNanos = System.nanoTime();
    private final boolean slowTime;
    private final double[] mouseXBuffer = new double[1];
    private final double[] mouseYBuffer = new double[1];
    private final float lockedPlayerYaw;
    private final float lockedPlayerPitch;
    private List<MagicGesturePoint> currentStroke;
    private MagicGestureSmoother smoother;
    private MagicGesturePoint cursor;
    private Vec3 lastWorld;
    private double pointerX;
    private double pointerY;
    private double lastMouseX;
    private double lastMouseY;
    private float lockedCameraYaw;
    private float lockedCameraPitch;
    private boolean cameraLockInitialized;
    private int pointCount;

    MagicGestureSession(MagicGestureMode mode, MagicGesturePlane plane,
                        Optional<MagicGestureSymbol> drawingSchool, boolean slowTime) {
        this.mode = mode;
        this.plane = plane;
        this.drawingSchool = drawingSchool;
        this.slowTime = slowTime;
        lockedPlayerYaw = 0.0F;
        lockedPlayerPitch = 0.0F;
    }

    MagicGestureSession(MagicGestureMode mode, MagicGesturePlane plane,
                        Optional<MagicGestureSymbol> drawingSchool, boolean slowTime,
                        Minecraft minecraft, LocalPlayer player) {
        this.mode = mode;
        this.plane = plane;
        this.drawingSchool = drawingSchool;
        this.slowTime = slowTime;
        lockedPlayerYaw = player.getYRot();
        lockedPlayerPitch = player.getXRot();
        readMouse(minecraft);
        lastMouseX = mouseXBuffer[0];
        lastMouseY = mouseYBuffer[0];
    }

    MagicGestureMode mode() { return mode; }
    Optional<MagicGestureSymbol> drawingSchool() { return drawingSchool; }
    boolean slowTime() { return slowTime; }
    boolean drawing() { return currentStroke != null; }
    long elapsedMillis() { return (System.nanoTime() - startedNanos) / 1_000_000L; }
    Optional<MagicGesturePoint> cursor() { return Optional.ofNullable(cursor); }
    Optional<Vec3> lastWorld() { return Optional.ofNullable(lastWorld); }
    List<List<MagicGesturePoint>> strokes() {
        return strokes.stream().filter(stroke -> stroke.size() >= 2).map(List::copyOf).toList();
    }
    List<List<MagicGesturePoint>> visibleStrokes() {
        return strokes.stream().filter(stroke -> !stroke.isEmpty()).map(List::copyOf).toList();
    }

    void updatePointer(Minecraft minecraft, MagicGestureHudConfig.Layout layout) {
        if (!layout.freezeCamera()) return;
        readMouse(minecraft);
        double guiScale = Math.max(1.0D, minecraft.getWindow().getGuiScale());
        double deltaX = clamp((mouseXBuffer[0] - lastMouseX) / guiScale, -512.0D, 512.0D);
        double deltaY = clamp((mouseYBuffer[0] - lastMouseY) / guiScale, -512.0D, 512.0D);
        lastMouseX = mouseXBuffer[0];
        lastMouseY = mouseYBuffer[0];
        double scale = Math.max(4.0D, layout.gestureScale());
        pointerX = clamp(pointerX + deltaX * layout.mouseSensitivity() / scale,
                -MagicGesturePlane.CANVAS_HALF_WIDTH, MagicGesturePlane.CANVAS_HALF_WIDTH);
        pointerY = clamp(pointerY - deltaY * layout.mouseSensitivity() / scale,
                -MagicGesturePlane.CANVAS_HALF_HEIGHT, MagicGesturePlane.CANVAS_HALF_HEIGHT);
    }

    Optional<MagicGesturePlane.Projection> project(LocalPlayer player, float partialTick,
                                                   MagicGestureHudConfig.Layout layout) {
        if (!layout.freezeCamera()) return plane.project(player, partialTick);
        return Optional.of(plane.projectLocal(player, partialTick, pointerX, pointerY));
    }

    void restorePlayerLook(LocalPlayer player, MagicGestureHudConfig.Layout layout) {
        if (!layout.freezeCamera()) return;
        player.setYRot(lockedPlayerYaw);
        player.setXRot(lockedPlayerPitch);
        player.yRotO = lockedPlayerYaw;
        player.xRotO = lockedPlayerPitch;
    }

    void lockCamera(ViewportEvent.ComputeCameraAngles event, MagicGestureHudConfig.Layout layout) {
        if (!layout.freezeCamera()) return;
        if (!cameraLockInitialized) {
            lockedCameraYaw = event.getYaw();
            lockedCameraPitch = event.getPitch();
            cameraLockInitialized = true;
        }
        event.setYaw(lockedCameraYaw);
        event.setPitch(lockedCameraPitch);
    }

    boolean shortTeleportPreview() {
        if (mode != MagicGestureMode.TECHNIQUE
                || drawingSchool.filter(value -> value == MagicGestureSymbol.VOID).isEmpty()
                || strokes.size() != 1 || currentStroke == null || currentStroke.size() < 2) return false;
        long duration = currentStroke.get(currentStroke.size() - 1).elapsedMillis()
                - currentStroke.get(0).elapsedMillis();
        if (duration < 180L) return false;
        double centerX = currentStroke.stream().mapToDouble(MagicGesturePoint::x).average().orElse(0.0D);
        double centerY = currentStroke.stream().mapToDouble(MagicGesturePoint::y).average().orElse(0.0D);
        return currentStroke.stream().filter(point -> Math.hypot(point.x() - centerX,
                point.y() - centerY) <= 0.16D).count() >= currentStroke.size() - 1L;
    }

    void startStroke(MagicGesturePlane.Projection projection) {
        if (drawing() || strokes.size() >= MAXIMUM_STROKES || pointCount >= CAPTURE_POINT_LIMIT) return;
        currentStroke = new ArrayList<>();
        smoother = new MagicGestureSmoother();
        strokes.add(currentStroke);
        lastWorld = projection.world();
        addPoint(projection, true);
    }

    void sample(MagicGesturePlane.Projection projection, boolean force) {
        if (drawing()) addPoint(projection, force);
    }

    void endStroke(MagicGesturePlane.Projection projection) {
        if (!drawing()) return;
        addPoint(projection, true);
        currentStroke = null;
    }

    void preview(MagicGesturePlane.Projection projection) {
        cursor = new MagicGesturePoint(projection.x(), projection.y(), Math.min(elapsedMillis(),
                MagicGestureRecognizer.MAXIMUM_DURATION_MILLIS));
        lastWorld = projection.world();
    }

    private void addPoint(MagicGesturePlane.Projection projection, boolean force) {
        preview(projection);
        long elapsed = Math.min(elapsedMillis(), MagicGestureRecognizer.MAXIMUM_DURATION_MILLIS);
        MagicGestureSmoother.Sample sample = smoother.filter(
                new MagicGesturePoint(projection.x(), projection.y(), elapsed), force);
        MagicGesturePoint point = sample.point();
        if (sample.corner() != null && !currentStroke.isEmpty()) {
            currentStroke.set(currentStroke.size() - 1, sample.corner());
        }
        MagicGesturePoint previous = currentStroke.isEmpty() ? null : currentStroke.get(currentStroke.size() - 1);
        if (!force && previous != null && point.distanceTo(previous) < POINT_SPACING
                && elapsed - previous.elapsedMillis() < 200L) return;
        if (pointCount >= CAPTURE_POINT_LIMIT) currentStroke.set(currentStroke.size() - 1, point);
        else { currentStroke.add(point); pointCount++; }
        lastWorld = projection.world();
    }

    private void readMouse(Minecraft minecraft) {
        GLFW.glfwGetCursorPos(minecraft.getWindow().getWindow(), mouseXBuffer, mouseYBuffer);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
