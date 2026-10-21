package io.github.kxyr0.echocutscene.forge.api.event;

import io.github.kxyr0.echocutscene.api.identity.cutscene.CutscenePlaybackId;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneOutcome;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneStatus;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

public final class CutsceneLifecycleEvent extends Event {
    private final ServerPlayer player;
    private final CutscenePlaybackId playbackId;
    private final CutsceneStatus status;
    private final CutsceneOutcome outcome;

    public CutsceneLifecycleEvent(
            ServerPlayer player,
            CutscenePlaybackId playbackId,
            CutsceneStatus status,
            CutsceneOutcome outcome
    ) {
        this.player = Objects.requireNonNull(player, "player");
        this.playbackId = Objects.requireNonNull(playbackId, "playbackId");
        this.status = Objects.requireNonNull(status, "status");
        this.outcome = Objects.requireNonNull(outcome, "outcome");
    }

    public ServerPlayer player() {
        return player;
    }

    public CutscenePlaybackId playbackId() {
        return playbackId;
    }

    public CutsceneStatus status() {
        return status;
    }

    public CutsceneOutcome outcome() {
        return outcome;
    }
}
