package io.github.kxyr0.echonpcanimationadapter.lifecycle;

import io.github.kxyr0.echoanimation.forge.api.AnimationService;
import io.github.kxyr0.echoanimation.forge.api.AnimationServiceBridge;
import io.github.kxyr0.echoanimation.forge.api.AnimationServiceLookupResult;
import io.github.kxyr0.echonavigation.api.identity.NavigationSubjectId;
import io.github.kxyr0.echonavigation.api.movement.NavigationGait;
import io.github.kxyr0.echonavigation.api.movement.NavigationSnapshot;
import io.github.kxyr0.echonavigation.api.movement.NavigationStatus;
import io.github.kxyr0.echonavigation.forge.api.lifecycle.NavigationLifecycleEvent;
import io.github.kxyr0.echonavigation.forge.api.service.EchoNavigationServiceBridge;
import io.github.kxyr0.echonavigation.forge.api.service.NavigationQueryService;
import io.github.kxyr0.echonpcanimationadapter.AnimationSubjectMapping;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.server.MinecraftServer;

/** Converts navigation movement into adapter-owned humanoid locomotion. */
public final class NpcNavigationLocomotionAnimator {
    private static final int RECONCILE_INTERVAL_TICKS = 10;
    private static final int RECONCILE_MOVEMENT_LIMIT = 512;

    private final LocomotionMotionTracker motion = new LocomotionMotionTracker();
    private final LocomotionPlaybackController playback;
    private final Map<String, String> moving = new LinkedHashMap<>();
    private final Map<String, String> stopQueued = new LinkedHashMap<>();
    private final Map<String, Double> playbackSpeeds = new HashMap<>();
    private final Map<String, NavigationGait> gaits = new HashMap<>();
    private long lastReconcileTick = Long.MIN_VALUE;
    private long adapterTick;

    public NpcNavigationLocomotionAnimator(NpcAnimationLifecycleAdapter lifecycle) {
        playback = new LocomotionPlaybackController(lifecycle, new AnimationSubjectMapping());
    }

    public void onNavigationLifecycle(NavigationLifecycleEvent event) {
        String subject = event.snapshot().subjectId().value();
        String move = event.snapshot().moveId().value();
        switch (event.kind()) {
            case MOVEMENT_STARTED, MOVEMENT_PROGRESSED -> startOrProgress(subject, move, event);
            case MOVEMENT_ARRIVED, MOVEMENT_STOPPED, MOVEMENT_FAILED -> complete(subject, move);
        }
    }

    public void tick(MinecraftServer server) {
        adapterTick++;
        Optional<AnimationService> animation = animationService(server);
        if (animation.isEmpty()) {
            return;
        }
        navigationQuery(server).ifPresent(query -> reconcile(animation.orElseThrow(), query, adapterTick));
        for (var entry : List.copyOf(moving.entrySet())) {
            String subject = entry.getKey();
            String move = entry.getValue();
            if (!motion.shouldAnimate(subject, adapterTick)) {
                playback.stop(animation.orElseThrow(), subject, move + "/stationary");
                continue;
            }
            playback.startOrUpdate(server, animation.orElseThrow(), subject, move, adapterTick,
                    gaits.getOrDefault(subject, NavigationGait.WALK), playbackSpeed(subject));
        }
        for (var entry : List.copyOf(stopQueued.entrySet())) {
            if (!moving.containsKey(entry.getKey())) {
                playback.stop(animation.orElseThrow(), entry.getKey(), entry.getValue());
            }
        }
    }

    private void startOrProgress(String subject, String move, NavigationLifecycleEvent event) {
        NavigationSnapshot snapshot = event.snapshot();
        gaits.put(subject, snapshot.gait());
        playbackSpeeds.put(subject, motion.update(subject, snapshot.progress(), snapshot.gait(),
                snapshot.synchronizePresentationSpeed(), adapterTick));
        moving.put(subject, move);
        stopQueued.remove(subject);
    }

    private void complete(String subject, String move) {
        if (move.equals(moving.get(subject))) {
            moving.remove(subject);
        }
        motion.clear(subject);
        playbackSpeeds.remove(subject);
        gaits.remove(subject);
        stopQueued.put(subject, move);
    }

    private Optional<AnimationService> animationService(MinecraftServer server) {
        AnimationServiceLookupResult lookup = AnimationServiceBridge.lookup(server);
        return lookup.status() == AnimationServiceLookupResult.Status.RESOLVED ? lookup.service() : Optional.empty();
    }

    private Optional<NavigationQueryService> navigationQuery(MinecraftServer server) {
        var lookup = EchoNavigationServiceBridge.lookup(server);
        return lookup.status() == EchoNavigationServiceBridge.Status.RESOLVED ? lookup.queryService() : Optional.empty();
    }

    private void reconcile(AnimationService animation, NavigationQueryService navigation, long tick) {
        if (tick - lastReconcileTick < RECONCILE_INTERVAL_TICKS) {
            return;
        }
        lastReconcileTick = tick;
        Set<String> activelyMoving = new HashSet<>();
        for (NavigationSnapshot snapshot : navigation.list(RECONCILE_MOVEMENT_LIMIT)) {
            if (animates(snapshot.status())) {
                String subject = snapshot.subjectId().value();
                activelyMoving.add(subject);
                moving.putIfAbsent(subject, snapshot.moveId().value());
                gaits.put(subject, snapshot.gait());
                stopQueued.remove(subject);
            }
        }
        for (var subjectId : animation.subjectsByOwner(NpcAnimationLifecycleAdapter.OWNER)) {
            String subject = subjectId.value();
            boolean shouldAnimate = activelyMoving.contains(subject)
                    || navigation.active(new NavigationSubjectId(subject)).map(snapshot -> animates(snapshot.status()))
                            .orElse(false);
            if (!shouldAnimate) {
                moving.remove(subject);
                playbackSpeeds.remove(subject);
                gaits.remove(subject);
                motion.clear(subject);
                playback.stop(animation, subject, "orphan/reconcile");
            }
        }
    }

    private boolean animates(NavigationStatus status) {
        return status == NavigationStatus.MOVING || status == NavigationStatus.REPLANNING;
    }

    private double playbackSpeed(String subject) {
        return playbackSpeeds.getOrDefault(subject, motion.speed(subject));
    }
}
