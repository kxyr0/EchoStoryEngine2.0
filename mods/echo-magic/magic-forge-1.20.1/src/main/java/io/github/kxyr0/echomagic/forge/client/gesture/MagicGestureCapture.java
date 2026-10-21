package io.github.kxyr0.echomagic.forge.client.gesture;

import io.github.kxyr0.echomagic.api.gesture.MagicGestureSymbol;
import io.github.kxyr0.echomagic.api.gesture.MagicGestureModifier;
import io.github.kxyr0.echomagic.api.gesture.MagicGestureTemplateTarget;
import io.github.kxyr0.echomagic.core.gesture.MagicGestureMode;
import io.github.kxyr0.echomagic.core.gesture.MagicGesturePoint;
import io.github.kxyr0.echomagic.core.gesture.MagicGestureRecognizer;
import io.github.kxyr0.echomagic.core.gesture.composition.MagicGestureComposition;
import io.github.kxyr0.echomagic.core.gesture.composition.MagicGestureCompositionRecognizer;
import io.github.kxyr0.echomagic.forge.client.MagicClientBootstrap;
import io.github.kxyr0.echomagic.forge.client.MagicKeyMappings;
import io.github.kxyr0.echomagic.forge.config.MagicClientConfig;
import io.github.kxyr0.echomagic.forge.config.MagicGestureHudConfig;
import io.github.kxyr0.echomagic.forge.network.MagicNetwork;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import io.github.kxyr0.echomagic.core.gesture.template.MagicGestureTemplateLibrary;
import io.github.kxyr0.echomagic.forge.network.gesture.MagicGestureTrainingStatusPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import org.lwjgl.glfw.GLFW;

public final class MagicGestureCapture {
    private static final long CAPTURE_LIMIT_MILLIS = 10_000L;
    private final MagicGestureCompositionRecognizer compositions = new MagicGestureCompositionRecognizer();
    private final MagicGestureTrainingClient training = new MagicGestureTrainingClient();
    private final MagicGestureCanvasRenderer canvas = new MagicGestureCanvasRenderer();
    private MagicGestureSession session;
    private boolean wasSchoolDown;
    private boolean wasTechniqueDown;

    public void renderFrame(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        boolean usable = minecraft.screen == null && minecraft.level != null && player != null
                && player.isAlive() && !player.isSpectator()
                && io.github.kxyr0.echomagic.forge.service.MagicFeatureAccess.client(player.getUUID());
        boolean schoolDown = usable && MagicKeyMappings.SELECT_SCHOOL.isDown();
        boolean techniqueDown = usable && MagicKeyMappings.DRAW_GESTURE.isDown();
        if (session == null && training.active() && MagicKeyMappings.RETRY_TRAINING_SAMPLE.consumeClick()) {
            training.undo();
        }
        if (session == null) {
            if (training.active()) {
                if (training.mode() == MagicGestureMode.SCHOOL && schoolDown && !wasSchoolDown) {
                    begin(player, partialTick, MagicGestureMode.SCHOOL);
                } else if (training.mode() == MagicGestureMode.TECHNIQUE
                        && techniqueDown && !wasTechniqueDown) begin(player, partialTick, MagicGestureMode.TECHNIQUE);
            } else if (schoolDown && !wasSchoolDown) begin(player, partialTick, MagicGestureMode.SCHOOL);
            else if (techniqueDown && !wasTechniqueDown) begin(player, partialTick, MagicGestureMode.TECHNIQUE);
        }
        if (session != null) {
            if (!usable) cancel();
            else {
                MagicGestureHudConfig.Layout layout = MagicGestureHudConfig.current();
                session.updatePointer(minecraft, layout);
                session.restorePlayerLook(player, layout);
                updateStroke(player, partialTick, leftMouseDown(minecraft), layout);
                if (!modeKeyDown(schoolDown, techniqueDown)
                        || session.elapsedMillis() >= CAPTURE_LIMIT_MILLIS) finish(player, partialTick, layout);
            }
        }
        wasSchoolDown = schoolDown;
        wasTechniqueDown = techniqueDown;
    }

    public boolean active() { return session != null; }
    public boolean shortTeleportPreview() { return session != null && session.shortTeleportPreview(); }
    public void beginTraining(MagicGestureTemplateTarget target) { cancel(); training.begin(target); }
    public void trainingStatus(MagicGestureTrainingStatusPacket packet) { training.status(packet); }
    public void updateTemplates(Map<MagicGestureTemplateTarget, MagicGestureTemplateLibrary.Template> values) {
        training.updateTemplates(values);
    }
    MagicGestureSession session() { return session; }
    MagicGestureTrainingClient training() { return training; }
    public void renderHud(RenderGuiOverlayEvent.Post event) { canvas.renderHud(event, session, training); }

    public void suppressAttackMapping() {
        if (!active()) return;
        var attack = Minecraft.getInstance().options.keyAttack;
        attack.setDown(false);
        while (attack.consumeClick()) {
            // LMB belongs to the gesture until N or V is released.
        }
    }

    public void restorePlayerLook() {
        Minecraft minecraft = Minecraft.getInstance();
        if (session != null && minecraft.player != null) {
            session.restorePlayerLook(minecraft.player, MagicGestureHudConfig.current());
        }
    }

    public void lockCamera(ViewportEvent.ComputeCameraAngles event) {
        if (session != null) session.lockCamera(event, MagicGestureHudConfig.current());
    }

    private void begin(LocalPlayer player, float partialTick, MagicGestureMode mode) {
        if (player == null) return;
        MagicGesturePlane.facing(player, partialTick).ifPresent(plane -> {
            boolean slowTime = MagicClientConfig.SLOW_TIME_WHILE_DRAWING.get();
            Optional<MagicGestureSymbol> activeSchool = MagicClientBootstrap.state().gestureSchool()
                    .flatMap(MagicGestureSymbol::fromSerialized);
            session = new MagicGestureSession(mode, plane, activeSchool, slowTime,
                    Minecraft.getInstance(), player);
            MagicNetwork.sendDrawingState(slowTime);
            String hint = mode == MagicGestureMode.SCHOOL ? "message.echo_magic.gesture.begin_school"
                    : "message.echo_magic.gesture.begin_technique";
            player.displayClientMessage(Component.translatable(hint), true);
        });
    }

    private void updateStroke(LocalPlayer player, float partialTick, boolean mouseDown,
                              MagicGestureHudConfig.Layout layout) {
        session.project(player, partialTick, layout).ifPresent(projection -> {
            session.preview(projection);
            if (mouseDown && !session.drawing()) session.startStroke(projection);
            else if (mouseDown) session.sample(projection, false);
            else if (session.drawing()) session.endStroke(projection);
        });
    }

    private void finish(LocalPlayer player, float partialTick, MagicGestureHudConfig.Layout layout) {
        if (session.drawing()) session.project(player, partialTick, layout)
                .ifPresent(session::endStroke);
        MagicGestureSession finished = session;
        session = null;
        if (finished.slowTime()) MagicNetwork.sendDrawingState(false);
        List<List<MagicGesturePoint>> strokes = finished.strokes();
        if (strokes.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.echo_magic.gesture.empty"), true);
            return;
        }
        List<MagicGesturePoint> primary = finished.mode() == MagicGestureMode.SCHOOL
                ? MagicGestureRecognizer.mergeStrokes(strokes) : strokes.get(0);
        if (training.active()) {
            training.submit(primary);
            return;
        }
        if (finished.mode() == MagicGestureMode.SCHOOL) finishSchool(finished);
        else finishTechnique(player, finished);
    }

    private static void finishSchool(MagicGestureSession finished) {
        MagicNetwork.sendGesture(finished.mode(), finished.strokes());
    }

    private void finishTechnique(LocalPlayer player, MagicGestureSession finished) {
        var personal = training.recognizeDetailed(MagicGestureMode.TECHNIQUE, finished.strokes().get(0));
        if (personal.ambiguous()) {
            player.displayClientMessage(Component.translatable("message.echo_magic.gesture.ambiguous"), true);
            MagicNetwork.sendGesture(finished.mode(), finished.strokes()); return;
        }
        Optional<io.github.kxyr0.echomagic.api.gesture.MagicTechnique> personalTechnique = personal.target()
                .flatMap(target -> target.technique());
        Optional<MagicGestureComposition> composition = compositions.recognize(finished.strokes(), personalTechnique);
        if (composition.isEmpty()) {
            player.displayClientMessage(Component.translatable(
                    "message.echo_magic.gesture.technique_unrecognized"), true);
            MagicNetwork.sendGesture(finished.mode(), finished.strokes());
            return;
        }
        MagicGestureComposition recognized = composition.orElseThrow();
        Component technique = Component.translatable(recognized.technique().translationKey());
        if (recognized.modifier() == MagicGestureModifier.NONE) {
            player.displayClientMessage(Component.translatable("message.echo_magic.technique.recognized",
                    technique), true);
        } else {
            player.displayClientMessage(Component.translatable("message.echo_magic.technique.composed",
                    technique, Component.translatable(recognized.modifier().translationKey())), true);
        }
        MagicNetwork.sendGesture(finished.mode(), finished.strokes());
    }

    private boolean modeKeyDown(boolean schoolDown, boolean techniqueDown) {
        return session.mode() == MagicGestureMode.SCHOOL ? schoolDown : techniqueDown;
    }

    private void cancel() {
        if (session != null && session.slowTime()) MagicNetwork.sendDrawingState(false);
        session = null;
    }

    private static boolean leftMouseDown(Minecraft minecraft) {
        return GLFW.glfwGetMouseButton(minecraft.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT)
                == GLFW.GLFW_PRESS;
    }
}
