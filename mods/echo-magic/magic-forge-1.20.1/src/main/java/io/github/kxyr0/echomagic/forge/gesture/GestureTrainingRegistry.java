package io.github.kxyr0.echomagic.forge.gesture;

import io.github.kxyr0.echomagic.api.gesture.MagicGestureTemplateTarget;
import io.github.kxyr0.echomagic.api.player.MagicPlayerRef;
import io.github.kxyr0.echomagic.core.gesture.MagicGestureMode;
import io.github.kxyr0.echomagic.core.gesture.MagicGesturePoint;
import io.github.kxyr0.echomagic.core.gesture.template.MagicGestureTemplateLibrary;
import io.github.kxyr0.echomagic.core.gesture.template.MagicGestureTrainingFeedback;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class GestureTrainingRegistry {
    private final MagicGestureTemplateLibrary library = new MagicGestureTemplateLibrary();
    private final Map<MagicPlayerRef, Map<MagicGestureTemplateTarget,
            MagicGestureTemplateLibrary.Template>> templates = new HashMap<>();
    private final Map<MagicPlayerRef, Pending> pending = new HashMap<>();

    public TrainingResult sample(MagicPlayerRef player, MagicGestureTemplateTarget target,
                                 List<MagicGesturePoint> points) {
        Pending progress = pending.get(player);
        if (progress == null || progress.target() != target) {
            progress = new Pending(target, new ArrayList<>());
            pending.put(player, progress);
        }
        MagicGestureTrainingFeedback feedback = library.assess(target, points, progress.samples());
        if (feedback != MagicGestureTrainingFeedback.ACCEPTED) {
            return new TrainingResult(target, progress.samples().size(), false, feedback);
        }
        progress.samples().add(List.copyOf(points));
        int count = progress.samples().size();
        if (count < MagicGestureTemplateLibrary.SAMPLE_COUNT) {
            return new TrainingResult(target, count, false, MagicGestureTrainingFeedback.ACCEPTED);
        }
        MagicGestureTemplateLibrary.Template template = library.train(target, progress.samples());
        templates.computeIfAbsent(player, ignored -> new EnumMap<>(MagicGestureTemplateTarget.class))
                .put(target, template);
        pending.remove(player);
        return new TrainingResult(target, count, true, MagicGestureTrainingFeedback.COMPLETE);
    }

    public TrainingResult undo(MagicPlayerRef player, MagicGestureTemplateTarget target) {
        Pending progress = pending.get(player);
        if (progress == null || progress.target() != target || progress.samples().isEmpty()) {
            return new TrainingResult(target, 0, false, MagicGestureTrainingFeedback.UNDONE);
        }
        progress.samples().remove(progress.samples().size() - 1);
        return new TrainingResult(target, progress.samples().size(), false, MagicGestureTrainingFeedback.UNDONE);
    }

    public Optional<MagicGestureTemplateTarget> recognize(MagicPlayerRef player, MagicGestureMode mode,
                                                           List<MagicGesturePoint> points) {
        return library.recognize(mode, points, templates(player).values());
    }

    public MagicGestureTemplateLibrary.Recognition recognizeDetailed(MagicPlayerRef player,
            MagicGestureMode mode, List<MagicGesturePoint> points) {
        return library.recognizeDetailed(mode, points, templates(player).values());
    }

    public Map<MagicGestureTemplateTarget, MagicGestureTemplateLibrary.Template> templates(MagicPlayerRef player) {
        return Map.copyOf(templates.getOrDefault(player, Map.of()));
    }

    public void restore(MagicPlayerRef player,
                        Map<MagicGestureTemplateTarget, MagicGestureTemplateLibrary.Template> values) {
        if (values.isEmpty()) templates.remove(player);
        else templates.put(player, new EnumMap<>(values));
    }

    public void playerLeft(MagicPlayerRef player) { pending.remove(player); templates.remove(player); }
    public void reset() { pending.clear(); templates.clear(); }

    public record TrainingResult(MagicGestureTemplateTarget target, int sampleCount, boolean complete,
                                 MagicGestureTrainingFeedback feedback) { }
    private record Pending(MagicGestureTemplateTarget target, List<List<MagicGesturePoint>> samples) { }
}
