package io.github.kxyr0.echodialoguecutscene.forge.wait;

import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneId;
import io.github.kxyr0.echocutscene.forge.api.service.CutsceneService;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.observation.DialogueObservationHandle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

final class PendingCutsceneWaits {
    private final Map<String, Pending> waits = new HashMap<>();

    void put(Pending wait) {
        remove(wait.waitKey());
        waits.put(wait.waitKey(), wait);
    }

    List<Pending> drainReady(ServerPlayer player, CutsceneService cutscene) {
        List<Pending> ready = new ArrayList<>();
        Iterator<Map.Entry<String, Pending>> iterator = waits.entrySet().iterator();
        while (iterator.hasNext()) {
            Pending pending = iterator.next().getValue();
            if (pending.playerId().equals(player.getUUID()) && pending.ready(player, cutscene)) {
                iterator.remove();
                pending.close();
                ready.add(pending);
            }
        }
        return List.copyOf(ready);
    }

    Optional<Pending> remove(String waitKey) {
        Pending removed = waits.remove(waitKey);
        if (removed != null) {
            removed.close();
        }
        return Optional.ofNullable(removed);
    }

    static final class Pending {
        private final UUID playerId;
        private final CutsceneId cutsceneId;
        private final DialogueSessionId sessionId;
        private final int revision;
        private final String waitKey;
        private final PendingKind kind;
        private DialogueObservationHandle handle = () -> { };

        Pending(
                UUID playerId,
                CutsceneId cutsceneId,
                DialogueSessionId sessionId,
                int revision,
                String waitKey,
                PendingKind kind
        ) {
            this.playerId = playerId;
            this.cutsceneId = cutsceneId;
            this.sessionId = sessionId;
            this.revision = revision;
            this.waitKey = waitKey;
            this.kind = kind;
        }

        UUID playerId() {
            return playerId;
        }

        CutsceneId cutsceneId() {
            return cutsceneId;
        }

        DialogueSessionId sessionId() {
            return sessionId;
        }

        int revision() {
            return revision;
        }

        String waitKey() {
            return waitKey;
        }

        boolean ready(ServerPlayer player, CutsceneService cutscene) {
            return switch (kind) {
                case DONE -> !cutscene.isPlaying(player, cutsceneId);
                case PREPARED -> cutscene.isPrepared(player, cutsceneId);
            };
        }

        void attach(DialogueObservationHandle observationHandle) {
            handle = observationHandle;
        }

        void close() {
            handle.close();
            handle = () -> { };
        }
    }

    enum PendingKind {
        DONE,
        PREPARED
    }
}
