package io.github.kxyr0.echonpcanimationadapter.lifecycle;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoanimation.api.animation.AnimationCommandId;
import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlayResult;
import io.github.kxyr0.echoanimation.api.animation.AnimationStopResult;
import io.github.kxyr0.echoanimation.api.animation.query.AnimationPlaybackQueryResult;
import io.github.kxyr0.echoanimation.api.provisioning.AnimationSubjectProvisioningResult;
import io.github.kxyr0.echoanimation.forge.api.AnimationService;
import io.github.kxyr0.echonavigation.api.movement.NavigationGait;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.service.NpcActorView;
import io.github.kxyr0.echonpcanimationadapter.AnimationSubjectMapping;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

final class LocomotionPlaybackController {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AnimationId WALK = AnimationId.parse("jdh:humanoid_walk");
    private static final AnimationId SPRINT = AnimationId.parse("jdh:humanoid_run");
    private static final int RETRY_TICKS = 10;

    private final NpcAnimationLifecycleAdapter lifecycle;
    private final AnimationSubjectMapping mapping;
    private final LocomotionNpcLookup npcs = new LocomotionNpcLookup();
    private final Map<String, Long> retryAfterTick = new HashMap<>();
    private final Map<String, String> lastFailure = new HashMap<>();
    private final Map<String, Double> appliedPlaybackSpeeds = new HashMap<>();
    private final Map<String, AnimationId> playing = new HashMap<>();
    private String sessionNonce = newSessionNonce();
    private long commandSequence;

    LocomotionPlaybackController(NpcAnimationLifecycleAdapter lifecycle, AnimationSubjectMapping mapping) {
        this.lifecycle = java.util.Objects.requireNonNull(lifecycle, "lifecycle");
        this.mapping = java.util.Objects.requireNonNull(mapping, "mapping");
    }

    void resetSession() {
        retryAfterTick.clear();
        lastFailure.clear();
        appliedPlaybackSpeeds.clear();
        playing.clear();
        sessionNonce = newSessionNonce();
        commandSequence = 0L;
    }

    void startOrUpdate( MinecraftServer server, AnimationService animation, String subject, String move, long tick,
            NavigationGait gait, double playbackSpeed ) {
        AnimationId selected = gait == NavigationGait.SPRINT ? SPRINT : WALK;
        AnimationId current = playing.get(subject);
        if (current != null && !active(animation, subject, current)) {
            playing.remove(subject);
            appliedPlaybackSpeeds.remove(subject);
            current = null;
        }
        if (current != null && !current.equals(selected)) {
            stop(animation, subject, move);
            current = null;
        }
        if (current != null) {
            updateSpeed(animation, subject, move, current, playbackSpeed);
            return;
        }
        if (active(animation, subject, selected)) {
            playing.put(subject, selected);
            appliedPlaybackSpeeds.putIfAbsent(subject, 1.0D);
            updateSpeed(animation, subject, move, selected, playbackSpeed);
            return;
        }
        stopOther(animation, subject, move, selected);
        start(server, animation, subject, move, tick, selected, playbackSpeed);
    }

    void stop(AnimationService animation, String subject, String move) {
        appliedPlaybackSpeeds.remove(subject);
        AnimationId known = playing.remove(subject);
        stop(animation, subject, move, known == null ? WALK : known);
        if (known == null) {
            stop(animation, subject, move, SPRINT);
        }
    }

    private void stop(AnimationService animation, String subject, String move, AnimationId animationId) {
        if (!active(animation, subject, animationId)) {
            return;
        }
        try {
            AnimationStopResult result = animation.stopAnimation(mapping.subjectId(new ActorId(subject)),
                    Optional.of(animationId), commandId("stop", subject, move + "/" + animationId.value()));
            if (result.status() != AnimationStopResult.Status.STOP_REQUESTED
                    && result.status() != AnimationStopResult.Status.ALREADY_APPLIED
                    && result.status() != AnimationStopResult.Status.PLAYBACK_NOT_FOUND) {
                logFailure(subject, result.diagnosticCode(), result.message());
                return;
            }
            lastFailure.remove(subject);
            LOGGER.info("ENPC-ANIM-LOCOMOTION-STOP actor={} move={} animation={}", subject, move, animationId);
        } catch (IllegalArgumentException exception) {
            logFailure(subject, "ENPC-ANIM-LOCOMOTION-ACTOR", exception.getMessage());
        }
    }

    private boolean active(AnimationService animation, String subject, AnimationId animationId) {
        try {
            AnimationPlaybackQueryResult query = animation.queryAnimation(mapping.subjectId(new ActorId(subject)),
                    animationId);
            return query.status() == AnimationPlaybackQueryResult.Status.ACTIVE;
        } catch (IllegalArgumentException exception) {
            logFailure(subject, "ENPC-ANIM-LOCOMOTION-QUERY", exception.getMessage());
            return false;
        }
    }

    private void start( MinecraftServer server, AnimationService animation, String subject, String move, long tick,
            AnimationId animationId, double playbackSpeed ) {
        if (retryAfterTick.getOrDefault(subject, 0L) > tick) {
            return;
        }
        Optional<NpcActorView> actor = npcs.find(server, subject);
        if (actor.isEmpty()) {
            return;
        }
        var provisioning = lifecycle.ensureActorSubject(actor.orElseThrow(), animation);
        if (provisioning.status() != AnimationSubjectProvisioningResult.Status.CREATED
                && provisioning.status() != AnimationSubjectProvisioningResult.Status.EXISTING) {
            retryAfterTick.put(subject, tick + RETRY_TICKS);
            logFailure(subject, provisioning.diagnosticCode(), provisioning.message());
            return;
        }
        ActorId actorId = actor.orElseThrow().actorId();
        AnimationPlayResult result = AnimationPlaybackSpeedBridge.play(animation, animationId,
                mapping.subjectId(actorId), commandId("play", subject, move), playbackSpeed);
        if (result.status() == AnimationPlayResult.Status.STARTED
                || result.status() == AnimationPlayResult.Status.ALREADY_APPLIED) {
            playing.put(subject, animationId);
            appliedPlaybackSpeeds.put(subject, playbackSpeed);
            lastFailure.remove(subject);
            LOGGER.info("ENPC-ANIM-LOCOMOTION-START actor={} move={} animation={} speed={}",
                    subject, move, animationId, playbackSpeed);
            return;
        }
        retryAfterTick.put(subject, tick + RETRY_TICKS);
        logFailure(subject, result.diagnosticCode(), result.message());
    }

    private void updateSpeed(AnimationService animation, String subject, String move, AnimationId animationId,
            double playbackSpeed) {
        double previous = appliedPlaybackSpeeds.getOrDefault(subject, 1.0D);
        if (Math.abs(previous - playbackSpeed) < 0.05D) {
            return;
        }
        try {
            boolean updated = AnimationPlaybackSpeedBridge.update(animation, mapping.subjectId(new ActorId(subject)),
                    animationId, commandId("speed", subject, move), playbackSpeed);
            if (updated) {
                appliedPlaybackSpeeds.put(subject, playbackSpeed);
                LOGGER.debug("ENPC-ANIM-LOCOMOTION-SPEED actor={} move={} speed={}", subject, move, playbackSpeed);
            }
        } catch (IllegalArgumentException exception) {
            logFailure(subject, "ENPC-ANIM-LOCOMOTION-SPEED", exception.getMessage());
        }
    }

    private void stopOther(AnimationService animation, String subject, String move, AnimationId selected) {
        AnimationId other = selected.equals(WALK) ? SPRINT : WALK;
        stop(animation, subject, move, other);
    }

    private AnimationCommandId commandId(String action, String subject, String move) {
        String sequence = Long.toUnsignedString(++commandSequence, 36);
        return AnimationCommandId.parse("echo_npc_animation:locomotion/" + action + "/"
                + path(subject) + "/" + path(move) + "/s" + sessionNonce + "/" + sequence);
    }

    private static String newSessionNonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String path(String value) {
        return value.replace(':', '/');
    }

    private void logFailure(String subject, String code, String message) {
        String fingerprint = code + "|" + message;
        if (!fingerprint.equals(lastFailure.put(subject, fingerprint))) {
            LOGGER.warn("ENPC-ANIM-LOCOMOTION-FAILED actor={} code={} message={}", subject, code, message);
        }
    }
}
