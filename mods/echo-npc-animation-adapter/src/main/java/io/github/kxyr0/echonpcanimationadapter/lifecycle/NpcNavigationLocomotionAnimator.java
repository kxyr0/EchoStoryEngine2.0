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
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.forge.api.navigation.EchoNpcNavigationBodyBridge;
import io.github.kxyr0.echonpc.forge.api.navigation.NpcNavigationBodyLookupResult;
import io.github.kxyr0.echonpcanimationadapter.AnimationSubjectMapping;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.server.MinecraftServer;

/** Drives the exclusive BODY locomotion state from real NPC motion. */
public final class NpcNavigationLocomotionAnimator {
    private static final int RECONCILE_INTERVAL_TICKS = 10;
    private static final int RECONCILE_MOVEMENT_LIMIT = 512;

    private final LocomotionMotionTracker bodyState = new LocomotionMotionTracker();
    private final LocomotionPlaybackController playback;
    private final Map<String, String> moving = new LinkedHashMap<>();
    private final Map<String, String> stopQueued = new LinkedHashMap<>();
    private final Map<String, NavigationGait> gaits = new HashMap<>();
    private long lastReconcileTick = Long.MIN_VALUE;
    private long adapterTick;
    private MinecraftServer sessionServer;

    public NpcNavigationLocomotionAnimator(NpcAnimationLifecycleAdapter lifecycle) {
        playback = new LocomotionPlaybackController(lifecycle, new AnimationSubjectMapping());
    }

    public void onNavigationLifecycle(NavigationLifecycleEvent event) {
        String subject = event.snapshot().subjectId().value();
        String move = event.snapshot().moveId().value();
        switch (event.kind()) {
            case MOVEMENT_STARTED, MOVEMENT_PROGRESSED -> startOrProgress(subject, move, event.snapshot());
            case MOVEMENT_ARRIVED, MOVEMENT_STOPPED, MOVEMENT_FAILED -> complete(subject, move);
        }
    }

    public void tick(MinecraftServer server) {
        beginServerSession(server);
        Optional<AnimationService> service = animationService(server);
        if (service.isEmpty()) {
            return;
        }
        AnimationService animation = service.orElseThrow();
        long tick = ++adapterTick;
        navigationQuery(server).ifPresent(query -> reconcile(animation, query, tick));
        for (var entry : List.copyOf(moving.entrySet())) {
            tickSubject(server, animation, entry.getKey(), entry.getValue(), tick);
        }
        flushStops(animation);
    }


    private void beginServerSession(MinecraftServer server) {
        if (sessionServer == server) {
            return;
        }
        bodyState.clearAll();
        playback.resetSession();
        moving.clear();
        stopQueued.clear();
        gaits.clear();
        lastReconcileTick = Long.MIN_VALUE;
        adapterTick = 0L;
        sessionServer = server;
    }

    private void tickSubject(
            MinecraftServer server,
            AnimationService animation,
            String subject,
            String move,
            long tick
    ) {
        Optional<BodySample> body = body(server, subject);
        if (body.isEmpty()) {
            stopLocomotion(animation, subject, move, "body-unavailable");
            return;
        }
        BodySample sample = body.orElseThrow();
        NavigationGait gait = gaits.getOrDefault(subject, NavigationGait.WALK);
        LocomotionMotionTracker.BodyState state = bodyState.observe(
                subject, sample.x(), sample.z(), tick, gait, sample.sitting());
        if (!state.locomoting()) {
            playback.stop(animation, subject, move + "/" + state.name().toLowerCase(java.util.Locale.ROOT));
            return;
        }
        playback.startOrUpdate(server, animation, subject, move, tick, gait, bodyState.speed(subject));
    }

    private void flushStops(AnimationService animation) {
        for (var entry : List.copyOf(stopQueued.entrySet())) {
            if (!moving.containsKey(entry.getKey())) {
                playback.stop(animation, entry.getKey(), entry.getValue());
                stopQueued.remove(entry.getKey(), entry.getValue());
            }
        }
    }

    private void stopLocomotion(AnimationService animation, String subject, String move, String reason) {
        bodyState.clear(subject);
        playback.stop(animation, subject, move + "/" + reason);
    }

    private Optional<BodySample> body(MinecraftServer server, String subject) {
        try {
            var lookup = EchoNpcNavigationBodyBridge.lookup(server, new ActorId(subject));
            if (lookup.status() != NpcNavigationBodyLookupResult.Status.RESOLVED || lookup.body().isEmpty()) {
                return Optional.empty();
            }
            var mob = lookup.body().orElseThrow().mob();
            return Optional.of(new BodySample(mob.getX(), mob.getZ(), mob.isPassenger()));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private void startOrProgress(String subject, String move, NavigationSnapshot snapshot) {
        gaits.put(subject, snapshot.gait());
        moving.put(subject, move);
        stopQueued.remove(subject);
    }

    private void complete(String subject, String move) {
        if (move.equals(moving.get(subject))) {
            moving.remove(subject);
        }
        bodyState.clear(subject);
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
        Set<String> active = new HashSet<>();
        for (NavigationSnapshot snapshot : navigation.list(RECONCILE_MOVEMENT_LIMIT)) {
            if (animates(snapshot.status())) {
                String subject = snapshot.subjectId().value();
                active.add(subject);
                moving.putIfAbsent(subject, snapshot.moveId().value());
                gaits.put(subject, snapshot.gait());
                stopQueued.remove(subject);
            }
        }
        for (var subjectId : animation.subjectsByOwner(NpcAnimationLifecycleAdapter.OWNER)) {
            String subject = subjectId.value();
            boolean navigationActive = active.contains(subject)
                    || navigation.active(new NavigationSubjectId(subject))
                            .map(snapshot -> animates(snapshot.status())).orElse(false);
            if (!navigationActive) {
                moving.remove(subject);
                gaits.remove(subject);
                stopLocomotion(animation, subject, "orphan", "navigation-idle");
            }
        }
    }

    private boolean animates(NavigationStatus status) {
        return status == NavigationStatus.MOVING || status == NavigationStatus.REPLANNING;
    }

    private record BodySample(double x, double z, boolean sitting) {
    }
}
