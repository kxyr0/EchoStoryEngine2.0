package io.github.kxyr0.echobackgroundnpc.forge.api.quest;

import io.github.kxyr0.echobackgroundnpc.api.conversation.AmbientQuestOffer;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Server-side bridge from a completed nearby ambient conversation to the project quest runtime. */
public interface AmbientQuestDispatcher {
    Result start(MinecraftServer server, ServerPlayer player, AmbientQuestOffer offer);

    record Result(Status status, String detail) {
        public Result {
            status = Objects.requireNonNull(status, "status");
            detail = Objects.requireNonNull(detail, "detail");
        }
    }

    enum Status { STARTED, UNAVAILABLE, REJECTED }
}
