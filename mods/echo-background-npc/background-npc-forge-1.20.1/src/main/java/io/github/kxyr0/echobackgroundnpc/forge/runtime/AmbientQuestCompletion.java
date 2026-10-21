package io.github.kxyr0.echobackgroundnpc.forge.runtime;

import io.github.kxyr0.echobackgroundnpc.api.conversation.ConversationOutcome;
import io.github.kxyr0.echobackgroundnpc.core.conversation.AmbientConversationSession;
import io.github.kxyr0.echobackgroundnpc.core.conversation.AmbientQuestEligibility;
import io.github.kxyr0.echobackgroundnpc.forge.api.quest.AmbientQuestDispatcher;
import java.util.function.Predicate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/** Starts completion quests only for players still observing the nearby conversation. */
final class AmbientQuestCompletion {
    private final AmbientQuestEligibility eligibility = new AmbientQuestEligibility();
    private final MinecraftServer server;
    private final MaterializedBodies bodies;
    private final AmbientQuestDispatcher dispatcher;
    private final Logger logger;

    AmbientQuestCompletion(MinecraftServer server, MaterializedBodies bodies,
            AmbientQuestDispatcher dispatcher, Logger logger) {
        this.server = server;
        this.bodies = bodies;
        this.dispatcher = dispatcher;
        this.logger = logger;
    }

    void complete(AmbientConversationSession session, Predicate<ServerPlayer> observed) {
        if (session.outcome().orElse(null) != ConversationOutcome.COMPLETED
                || session.dialogue().completionQuest().isEmpty()) {
            return;
        }
        var first = bodies.find(session.first());
        var second = bodies.find(session.second());
        if (first.isEmpty() || second.isEmpty()) return;
        var offer = session.dialogue().completionQuest().orElseThrow();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            boolean sameDimension = player.level() == first.orElseThrow().level()
                    && player.level() == second.orElseThrow().level();
            boolean sight = sameDimension && (player.hasLineOfSight(first.orElseThrow())
                    || player.hasLineOfSight(second.orElseThrow()));
            if (!eligibility.eligible(observed.test(player), sameDimension, sight,
                    player.distanceToSqr(first.orElseThrow()), player.distanceToSqr(second.orElseThrow()),
                    offer.maximumDistance())) {
                continue;
            }
            var result = dispatcher.start(server, player, offer);
            logger.info("[BG_NPC] Ambient quest={} player={} status={} detail={}", offer.questId(),
                    player.getUUID(), result.status(), result.detail());
        }
    }
}
