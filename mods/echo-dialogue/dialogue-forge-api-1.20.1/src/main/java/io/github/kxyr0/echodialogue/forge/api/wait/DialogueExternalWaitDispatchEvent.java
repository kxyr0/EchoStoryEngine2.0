package io.github.kxyr0.echodialogue.forge.api.wait;

import io.github.kxyr0.echodialogue.api.session.DialogueSessionView;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWait;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Posted after a WAITING dialogue checkpoint has been durably saved.
 */
public final class DialogueExternalWaitDispatchEvent extends Event {
    private final Optional<ServerPlayer> player;
    private final DialogueSessionView session;
    private final DialogueExternalWait externalWait;

    public DialogueExternalWaitDispatchEvent(
            Optional<ServerPlayer> player,
            DialogueSessionView session,
            DialogueExternalWait externalWait
    ) {
        this.player = Objects.requireNonNull(player, "player");
        this.session = Objects.requireNonNull(session, "session");
        this.externalWait = Objects.requireNonNull(externalWait, "externalWait");
    }

    public Optional<ServerPlayer> player() {
        return player;
    }

    public DialogueSessionView session() {
        return session;
    }

    public DialogueExternalWait externalWait() {
        return externalWait;
    }
}
