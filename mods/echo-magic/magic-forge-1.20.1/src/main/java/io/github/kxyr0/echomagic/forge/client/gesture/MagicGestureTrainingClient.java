package io.github.kxyr0.echomagic.forge.client.gesture;

import io.github.kxyr0.echomagic.api.gesture.MagicGestureTemplateTarget;
import io.github.kxyr0.echomagic.core.gesture.MagicGestureMode;
import io.github.kxyr0.echomagic.core.gesture.MagicGesturePoint;
import io.github.kxyr0.echomagic.core.gesture.template.MagicGestureTemplateLibrary;
import io.github.kxyr0.echomagic.core.gesture.template.MagicGestureTrainingFeedback;
import io.github.kxyr0.echomagic.forge.network.MagicNetwork;
import io.github.kxyr0.echomagic.forge.network.gesture.MagicGestureTrainingStatusPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

final class MagicGestureTrainingClient {
    private final MagicGestureTemplateLibrary library = new MagicGestureTemplateLibrary();
    private final List<List<MagicGesturePoint>> accepted = new ArrayList<>();
    private Map<MagicGestureTemplateTarget, MagicGestureTemplateLibrary.Template> templates = Map.of();
    private MagicGestureTemplateTarget target;
    private List<MagicGesturePoint> pending;
    private long reviewUntilNanos;

    void begin(MagicGestureTemplateTarget newTarget) {
        target = newTarget; accepted.clear(); pending = null; reviewUntilNanos = 0L;
        message("message.echo_magic.training.started", label(newTarget), key(newTarget));
    }

    boolean active() { return target != null; }
    boolean reviewing() { return !accepted.isEmpty() && System.nanoTime() < reviewUntilNanos; }
    Optional<MagicGestureTemplateTarget> target() { return Optional.ofNullable(target); }
    List<List<MagicGesturePoint>> accepted() { return accepted.stream().map(List::copyOf).toList(); }
    MagicGestureMode mode() { return target != null && target.school()
            ? MagicGestureMode.SCHOOL : MagicGestureMode.TECHNIQUE; }

    void submit(List<MagicGesturePoint> points) {
        if (target == null || pending != null) return;
        pending = List.copyOf(points); MagicNetwork.sendTrainingSample(target, points);
    }

    void undo() {
        if (target != null && pending == null && !accepted.isEmpty()) MagicNetwork.undoTrainingSample(target);
    }

    void status(MagicGestureTrainingStatusPacket packet) {
        if (target != packet.target()) return;
        MagicGestureTrainingFeedback feedback = packet.feedback();
        if (feedback == MagicGestureTrainingFeedback.ACCEPTED
                || feedback == MagicGestureTrainingFeedback.COMPLETE) acceptPending();
        else pending = null;
        if (feedback == MagicGestureTrainingFeedback.COMPLETE) {
            message("message.echo_magic.training.complete", label(packet.target()));
            reviewUntilNanos = System.nanoTime() + 3_000_000_000L; target = null;
        } else if (feedback == MagicGestureTrainingFeedback.ACCEPTED) {
            message("message.echo_magic.training.progress", label(packet.target()), packet.sampleCount(),
                    MagicGestureTemplateLibrary.SAMPLE_COUNT, key(packet.target()));
        } else if (feedback == MagicGestureTrainingFeedback.UNDONE) {
            while (accepted.size() > packet.sampleCount()) accepted.remove(accepted.size() - 1);
            message("message.echo_magic.training.undone", packet.sampleCount(),
                    MagicGestureTemplateLibrary.SAMPLE_COUNT);
        } else {
            message("message.echo_magic.training." + feedback.name().toLowerCase(java.util.Locale.ROOT));
        }
    }

    private void acceptPending() {
        if (pending != null) accepted.add(library.normalize(pending));
        pending = null;
    }

    double consistency(List<MagicGesturePoint> points) { return library.consistency(points, accepted); }

    void updateTemplates(Map<MagicGestureTemplateTarget, MagicGestureTemplateLibrary.Template> values) {
        templates = Map.copyOf(values);
    }

    Optional<MagicGestureTemplateTarget> recognize(MagicGestureMode mode, List<MagicGesturePoint> points) {
        return library.recognize(mode, points, templates.values());
    }
    MagicGestureTemplateLibrary.Recognition recognizeDetailed(MagicGestureMode mode,
                                                               List<MagicGesturePoint> points) {
        return library.recognizeDetailed(mode, points, templates.values());
    }

    private static Component label(MagicGestureTemplateTarget value) {
        return Component.translatable(value.translationKey());
    }

    private static Component key(MagicGestureTemplateTarget value) {
        return Component.literal(value.school() ? "N" : "V");
    }

    private static void message(String key, Object... arguments) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(Component.translatable(key, arguments), true);
        }
    }
}
