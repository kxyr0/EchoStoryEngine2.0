package io.github.kxyr0.echoanimation.echoscript.procedural.animation;

import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPhase;
import io.github.kxyr0.echoanimation.api.animation.definition.AnimationBlendPolicy;
import io.github.kxyr0.echoanimation.api.animation.definition.AnimationDefinition;
import io.github.kxyr0.echoanimation.api.animation.definition.MainMode;
import io.github.kxyr0.echoanimation.api.animation.definition.RecoveryPolicy;
import io.github.kxyr0.echoanimation.api.animation.definition.ReplayPolicy;
import io.github.kxyr0.echoanimation.api.animation.marker.AnimationMarkerDefinition;
import io.github.kxyr0.echoanimation.api.animation.marker.AnimationMarkerId;
import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import io.github.kxyr0.echoanimation.definition.fingerprint.SemanticFingerprinter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Semantic accumulator for independent Enter/Loop/Hold/OneShot/Exit declarations. */
final class AnimationDslDraft {
    private String id;
    private final Map<String, AnimationDslValue> locals = new LinkedHashMap<>();
    private final Set<AnimationChannelId> channels = new LinkedHashSet<>();
    private final List<AnimationDefinition.EntryVariant> entries = new ArrayList<>();
    private final List<AnimationDefinition.ExitVariant> exits = new ArrayList<>();
    private final List<AnimationDefinition.ExitPoint> exitPoints = new ArrayList<>();
    private final List<AnimationMarkerDefinition> markers = new ArrayList<>();
    private AnimationDslValue.Play mainPlay;
    private MainMode mainMode;
    private AnimationStateId mainState;
    private AnimationStateId target;
    private RecoveryPolicy recovery = RecoveryPolicy.BOUNDED;
    private ReplayPolicy replay = ReplayPolicy.REJECT_IF_RUNNING;
    private int checkpointTicks;
    private boolean holdLastFrame;
    private int fallbackTicks;
    private int blendInTicks;
    private int blendOutTicks;
    private Optional<String> maskId = Optional.empty();

    void id(String value) {
        if (id != null) {
            throw new IllegalArgumentException("Id is declared twice.");
        }
        id = value;
    }

    void local(String variable, AnimationDslValue value) {
        if (locals.putIfAbsent(variable, value) != null) {
            throw new IllegalArgumentException( "Local variable '" + variable + "' is declared twice.");
        }
        if (value instanceof AnimationDslValue.Channel channel) {
            channels.add(channel.id());
        }
        if (value instanceof AnimationDslValue.State state) {
            channels.add(state.channel());
        }
    }

    AnimationDslValue local(String variable) {
        return Optional.ofNullable(locals.get(variable)) .orElseThrow( () ->
                                new IllegalArgumentException( "Unknown local variable '" + variable + "'."));
    }

    void enter( AnimationDslValue.State state, AnimationDslValue.State source, AnimationDslValue.Play play) {
        channels.add(state.channel());
        if (entries.stream().anyMatch(entry -> entry.sourceState().equals(source.id()))) {
            throw new IllegalArgumentException( "State '" + source.id() + "' has two Enter declarations.");
        }
        entries.add( new AnimationDefinition.EntryVariant(source.id(), play.clip(), play.segment(), 0L));
    }

    void loop(AnimationDslValue.State state, AnimationDslValue.Play play) {
        main(state.channel(), state.id(), play, MainMode.LOOP_UNTIL_STOP);
    }

    void hold(AnimationDslValue.State state, AnimationDslValue.Play pose) {
        if (pose.point().isEmpty()) {
            throw new IllegalArgumentException("Hold requires a named clip point.");
        }
        main(state.channel(), state.id(), pose, MainMode.HOLD_UNTIL_STOP);
    }

    void oneShot(AnimationChannelId channel, AnimationDslValue.Play play) {
        main(channel, new AnimationStateId(channel.value() + "/oneshot"), play, MainMode.ONCE);
    }

    void exit( AnimationDslValue.State state, AnimationDslValue.State destination, AnimationDslValue.Play play,
            Optional<AnimationDslValue.Play> at) {
        channels.add(state.channel());
        String pointId = at.flatMap(AnimationDslValue.Play::point).orElse("__end");
        if (exits.stream().anyMatch(exit -> exit.exitPointId().equals(pointId))) {
            throw new IllegalArgumentException(
                    "State '" + state.id() + "' has two Exit declarations at '" + pointId + "'.");
        }
        exits.add(new AnimationDefinition.ExitVariant(pointId, play.clip(), play.segment(), 0L));
        double progress = at.isPresent() ? 0.0D : 1.0D;
        exitPoints.add( new AnimationDefinition.ExitPoint(
                        pointId, progress, at.flatMap(AnimationDslValue.Play::point)));
        target = destination.id();
    }

    void returnTo(AnimationStateId state) {
        target = state;
    }

    void holdLastFrame() {
        holdLastFrame = true;
    }

    void marker(String markerId, AnimationPhase phase, long tick) {
        if (id == null) { throw new IllegalArgumentException("Marker requires Id to be declared first."); }
        String resolvedId = markerId.contains(":") ? markerId : id.substring(0, id.indexOf(':') + 1) + markerId;
        AnimationMarkerDefinition marker =
                new AnimationMarkerDefinition(AnimationMarkerId.parse(resolvedId), phase, tick);
        if (markers.stream().anyMatch(existing -> existing.id().equals(marker.id()))) {
            throw new IllegalArgumentException( "Animation marker '" + resolvedId + "' is declared twice.");
        }
        markers.add(marker);
    }

    void recovery(RecoveryPolicy policy, int checkpointEvery) {
        recovery = policy;
        checkpointTicks = checkpointEvery;
    }

    void replay(ReplayPolicy policy) {
        replay = policy;
    }

    void fallback(int ticks) {
        fallbackTicks = once(fallbackTicks, ticks, "Fallback");
    }

    void blendIn(int ticks) {
        blendInTicks = once(blendInTicks, ticks, "BlendIn");
    }

    void blendOut(int ticks) {
        blendOutTicks = once(blendOutTicks, ticks, "BlendOut");
    }

    void blend(int ticks) {
        blendIn(ticks);
        blendOut(ticks);
    }

    void mask(String value) {
        if (maskId.isPresent()) {
            throw new IllegalArgumentException("Mask is declared twice.");
        }
        maskId = Optional.of(value);
    }

    AnimationDefinition build() {
        if (id == null || mainPlay == null || channels.isEmpty()) {
            throw new IllegalArgumentException( "Procedural animation requires Id, a channel and Loop/Hold/OneShot.");
        }
        MainMode mode = holdLastFrame && mainMode == MainMode.ONCE ? MainMode.HOLD_UNTIL_STOP : mainMode;
        AnimationDefinition.MainPhase main = new AnimationDefinition.MainPhase( mainPlay.clip(), mainPlay.segment(),
                        mainPlay.point(), mode, 0L, exitPoints);
        Set<AnimationStateId> sources = entries.stream() .map(AnimationDefinition.EntryVariant::sourceState)
                        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        AnimationStateId resolvedTarget = target != null ? target : mainState;
        String fingerprint = new SemanticFingerprinter() .fingerprint( List.of( id, channels.toString(),
                                        entries.toString(), main.toString(), exits.toString(), markers.toString(),
                                        recovery.name(), replay.name(), fallbackTicks + ":" + blendInTicks + ":"
                                                + blendOutTicks + ":" + maskId));
        AnimationBlendPolicy blends = new AnimationBlendPolicy(fallbackTicks, blendInTicks, blendOutTicks, maskId);
        return new AnimationDefinition( AnimationId.parse(id), channels, sources, entries, main, exits, markers,
                resolvedTarget, recovery, checkpointTicks, replay, blends, fingerprint);
    }

    private void main( AnimationChannelId channel, AnimationStateId state, AnimationDslValue.Play play, MainMode mode) {
        if (mainPlay != null) {
            throw new IllegalArgumentException(
                    "State '" + state + "' has conflicting Hold/Loop/OneShot declarations.");
        }
        channels.add(channel);
        mainState = state;
        mainPlay = play;
        mainMode = mode;
    }

    private int once(int current, int value, String name) {
        if (current > 0 || value <= 0) {
            throw new IllegalArgumentException(name + " ticks must be positive and declared once.");
        }
        return value;
    }
}
