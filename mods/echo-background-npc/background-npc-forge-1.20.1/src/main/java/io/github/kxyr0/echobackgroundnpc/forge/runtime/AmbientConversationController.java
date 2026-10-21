package io.github.kxyr0.echobackgroundnpc.forge.runtime;

import io.github.kxyr0.echobackgroundnpc.api.conversation.ConversationOutcome;
import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;
import io.github.kxyr0.echobackgroundnpc.api.runtime.AgentLifeState;
import io.github.kxyr0.echobackgroundnpc.api.task.TaskSource;
import io.github.kxyr0.echobackgroundnpc.core.control.BackgroundBehaviorLease;
import io.github.kxyr0.echobackgroundnpc.core.conversation.AmbientConversationSession;
import io.github.kxyr0.echobackgroundnpc.core.conversation.AmbientReadingTime;
import io.github.kxyr0.echobackgroundnpc.core.simulation.BackgroundAgentRuntime;
import io.github.kxyr0.echobackgroundnpc.core.simulation.ScheduledConversation;
import io.github.kxyr0.echobackgroundnpc.forge.entity.BackgroundNpcEntity;
import io.github.kxyr0.echobackgroundnpc.forge.network.BackgroundNpcNetwork;
import io.github.kxyr0.echobackgroundnpc.forge.api.quest.AmbientQuestDispatcher;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/** Owns short revision-pinned, nonblocking two-agent conversations. */
public final class AmbientConversationController {
    private static final double RANGE_SQUARED = 14.0D * 14.0D;
    private final AmbientReadingTime timing = AmbientReadingTime.defaults();
    private final Map<String, AmbientConversationSession> sessions = new HashMap<>();
    private final Map<UUID, Shown> shown = new HashMap<>();
    private final MinecraftServer server;
    private final BackgroundAgentRuntime runtime;
    private final MaterializedBodies bodies;
    private final ConversationAnimator animator;
    private final AmbientQuestCompletion questCompletion;
    private final Logger logger;

    public AmbientConversationController(MinecraftServer server, BackgroundAgentRuntime runtime,
            MaterializedBodies bodies, ConversationAnimator animator, AmbientQuestDispatcher quests, Logger logger) {
        this.server = server;
        this.runtime = runtime;
        this.bodies = bodies;
        this.animator = animator;
        this.logger = logger;
        questCompletion = new AmbientQuestCompletion(server, bodies, quests, logger);
    }

    public void start(ScheduledConversation scheduled, long serverTick) {
        if (scheduled.outcome() != ConversationOutcome.COMPLETED) return;
        var dialogue = runtime.catalog().dialogues().get(scheduled.task().dialogue());
        var first = runtime.state(scheduled.initiator());
        var second = runtime.state(scheduled.task().participant());
        if (dialogue == null || first.isEmpty() || second.isEmpty()
                || second.orElseThrow().life() != AgentLifeState.ALIVE) {
            skipped(scheduled, ConversationOutcome.SKIPPED_PARTICIPANT_UNAVAILABLE); return;
        }
        var firstBody = bodies.find(scheduled.initiator());
        var secondBody = bodies.find(scheduled.task().participant());
        if (firstBody.isEmpty() || secondBody.isEmpty() || firstBody.orElseThrow().level() != secondBody.orElseThrow().level()
                || firstBody.orElseThrow().distanceToSqr(secondBody.orElseThrow()) > RANGE_SQUARED) {
            skipped(scheduled, ConversationOutcome.SKIPPED_PARTICIPANT_UNAVAILABLE); return;
        }
        String id = "echo_background_npc:" + Integer.toUnsignedString(scheduled.occurrenceId().hashCode());
        var lease = new BackgroundBehaviorLease(id, TaskSource.AMBIENT_CONVERSATION, dialogue.revision());
        if (!runtime.leases().acquire(scheduled.initiator(), lease)
                || !runtime.leases().acquire(scheduled.task().participant(), lease)) {
            runtime.leases().release(scheduled.initiator(), id);
            skipped(scheduled, ConversationOutcome.SKIPPED_PARTICIPANT_UNAVAILABLE); return;
        }
        var session = new AmbientConversationSession(id, scheduled.initiator(), scheduled.task().participant(),
                dialogue, serverTick * 50L, timing);
        sessions.put(id, session);
        pose(session);
    }

    public void tick(long serverTick) {
        long now = serverTick * 50L;
        for (AmbientConversationSession session : List.copyOf(sessions.values())) {
            session.advance(now, timing).ifPresent(ignored -> pose(session));
            if (!session.active()) finish(session);
        }
        if (serverTick % 4L == 0L) selectOverlays(now);
    }

    public void preemptFor(BackgroundNpcId id) {
        sessions.values().stream().filter(value -> value.first().equals(id) || value.second().equals(id))
                .toList().forEach(value -> { value.preempt(); finish(value); });
    }

    public void close() {
        List.copyOf(sessions.values()).forEach(this::finish);
        server.getPlayerList().getPlayers().forEach(player -> Optional.ofNullable(shown.remove(player.getUUID()))
                .ifPresent(value -> BackgroundNpcNetwork.clear(player, value.presentationId())));
    }

    private void pose(AmbientConversationSession session) {
        BackgroundNpcId speaker = speaker(session);
        BackgroundNpcEntity first = bodies.find(session.first()).orElseThrow();
        BackgroundNpcEntity second = bodies.find(session.second()).orElseThrow();
        face(first, second); face(second, first);
        first.setPresentationTask(speaker.equals(session.first()) ? "CONVERSE_TALK" : "CONVERSE_LISTEN");
        second.setPresentationTask(speaker.equals(session.second()) ? "CONVERSE_TALK" : "CONVERSE_LISTEN");
        animator.pose(session.first(), speaker.equals(session.first()));
        animator.pose(session.second(), speaker.equals(session.second()));
    }

    private void selectOverlays(long now) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Optional<Candidate> selected = sessions.values().stream().filter(AmbientConversationSession::active)
                    .map(value -> candidate(player, value)).flatMap(Optional::stream)
                    .min(Comparator.comparingDouble(Candidate::distanceSquared).thenComparing(value -> value.session().id()));
            updateOverlay(player, selected, now);
        }
    }

    private Optional<Candidate> candidate(ServerPlayer player, AmbientConversationSession session) {
        var entity = bodies.find(speaker(session));
        if (entity.isEmpty() || entity.orElseThrow().level() != player.level()) return Optional.empty();
        double distance = player.distanceToSqr(entity.orElseThrow());
        return distance <= RANGE_SQUARED && player.hasLineOfSight(entity.orElseThrow())
                ? Optional.of(new Candidate(session, distance, entity.orElseThrow())) : Optional.empty();
    }

    private void updateOverlay(ServerPlayer player, Optional<Candidate> selected, long now) {
        Shown previous = shown.get(player.getUUID());
        if (selected.isEmpty()) {
            if (previous != null) BackgroundNpcNetwork.clear(player, previous.presentationId());
            shown.remove(player.getUUID()); return;
        }
        var session = selected.orElseThrow().session();
        Shown next = new Shown(session.id(), session.id() + "/" + session.lineIndex());
        if (next.equals(previous)) return;
        BackgroundNpcNetwork.show(player, session.id(), session.dialogue().id().value(), session.current(),
                Math.min(60_000L, session.remainingMillis(now)), selected.orElseThrow().speaker());
        shown.put(player.getUUID(), next);
    }

    private void finish(AmbientConversationSession session) {
        questCompletion.complete(session, player -> {
            Shown view = shown.get(player.getUUID());
            return view != null && view.presentationId().equals(session.id());
        });
        sessions.remove(session.id());
        runtime.leases().release(session.first(), session.id());
        runtime.leases().release(session.second(), session.id());
        animator.restore(session.first()); animator.restore(session.second());
    }

    private BackgroundNpcId speaker(AmbientConversationSession session) {
        String key = session.current().speaker().toLowerCase(java.util.Locale.ROOT);
        return matches(session.first(), key) ? session.first() : session.second();
    }

    private boolean matches(BackgroundNpcId id, String speaker) {
        String value = id.value().substring(id.value().indexOf(':') + 1).toLowerCase(java.util.Locale.ROOT);
        return value.equals(speaker) || value.endsWith("_" + speaker);
    }

    private void face(BackgroundNpcEntity from, BackgroundNpcEntity to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        from.setYRot((float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F);
    }

    private void skipped(ScheduledConversation value, ConversationOutcome outcome) {
        logger.info("[BG_NPC] Conversation {} with {} outcome={} occurrence={}", value.initiator(),
                value.task().participant(), outcome, value.occurrenceId());
    }

    public interface ConversationAnimator {
        void pose(BackgroundNpcId id, boolean speaking);
        void restore(BackgroundNpcId id);
    }
    private record Candidate(AmbientConversationSession session, double distanceSquared,
            BackgroundNpcEntity speaker) { }
    private record Shown(String presentationId, String lineToken) { }
}
