package io.github.kxyr0.echobackgroundnpc.quest;

import io.github.kxyr0.echobackgroundnpc.api.conversation.AmbientQuestOffer;
import io.github.kxyr0.echobackgroundnpc.forge.api.quest.AmbientQuestDispatcher;
import io.github.kxyr0.echoquests.api.identity.QuestId;
import io.github.kxyr0.echoquests.api.runtime.QuestStartResult;
import io.github.kxyr0.echoquests.forge.api.EchoQuestServiceAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Starts one durable player-scoped quest after ambient eligibility has been checked. */
public final class EchoQuestAmbientQuestDispatcher implements AmbientQuestDispatcher {
    @Override
    public Result start(MinecraftServer server, ServerPlayer player, AmbientQuestOffer offer) {
        try {
            var service = EchoQuestServiceAccess.find(server);
            if (service.isEmpty()) return new Result(Status.UNAVAILABLE, "Echo Quests service is unavailable.");
            QuestStartResult result = service.orElseThrow().start(player.getUUID(), questId(offer));
            return result.status() == QuestStartResult.Status.UNKNOWN_QUEST
                    ? new Result(Status.REJECTED, result.detail()) : new Result(Status.STARTED, result.detail());
        } catch (RuntimeException exception) {
            return new Result(Status.REJECTED, exception.getClass().getSimpleName());
        }
    }

    static QuestId questId(AmbientQuestOffer offer) { return QuestId.parse(offer.questId().value()); }
}
