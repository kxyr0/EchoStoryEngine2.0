package io.github.kxyr0.echoplayersleep.forge.interception;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class StoryWakePermit {
    private static final ThreadLocal<Map<UUID, Integer>> PERMITS = ThreadLocal.withInitial(HashMap::new);

    private StoryWakePermit() {
    }

    public static Scope open(UUID player) {
        UUID checkedPlayer = Objects.requireNonNull(player, "player");
        Map<UUID, Integer> permits = PERMITS.get();
        permits.put(checkedPlayer, permits.getOrDefault(checkedPlayer, 0) + 1);
        return new Scope(checkedPlayer);
    }

    public static boolean isAllowed(UUID player) {
        return PERMITS.get().getOrDefault(Objects.requireNonNull(player, "player"), 0) > 0;
    }

    public static final class Scope implements AutoCloseable {
        private final UUID player;
        private boolean closed;

        private Scope(UUID player) {
            this.player = player;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            Map<UUID, Integer> permits = PERMITS.get();
            int nextDepth = permits.getOrDefault(player, 0) - 1;
            if (nextDepth <= 0) {
                permits.remove(player);
            } else {
                permits.put(player, nextDepth);
            }
            if (permits.isEmpty()) {
                PERMITS.remove();
            }
        }
    }
}
