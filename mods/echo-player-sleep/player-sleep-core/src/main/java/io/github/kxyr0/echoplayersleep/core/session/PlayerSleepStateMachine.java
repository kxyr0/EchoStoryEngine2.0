package io.github.kxyr0.echoplayersleep.core.session;

import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepStatus;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class PlayerSleepStateMachine {
    private final Map<PlayerSleepStatus, Set<PlayerSleepStatus>> allowed = new EnumMap<>(PlayerSleepStatus.class);

    public PlayerSleepStateMachine() {
        allow(PlayerSleepStatus.CREATED, PlayerSleepStatus.WAITING_FOR_VANILLA_SLEEP);
        allow(PlayerSleepStatus.WAITING_FOR_VANILLA_SLEEP, PlayerSleepStatus.VANILLA_SLEEP_CONFIRMED);
        allow(PlayerSleepStatus.VANILLA_SLEEP_CONFIRMED, PlayerSleepStatus.WAITING_FOR_STORY_CLAIM);
        allow(PlayerSleepStatus.WAITING_FOR_STORY_CLAIM, PlayerSleepStatus.STORY_CLAIMED);
        allow(PlayerSleepStatus.STORY_CLAIMED, PlayerSleepStatus.CLOSING_EYES);
        allow(PlayerSleepStatus.CLOSING_EYES, PlayerSleepStatus.EYES_CLOSED);
        allow(PlayerSleepStatus.EYES_CLOSED, PlayerSleepStatus.STORY_CLAIMED);
        allow(PlayerSleepStatus.EYES_CLOSED, PlayerSleepStatus.ENTERING_DREAM);
        allow(PlayerSleepStatus.ENTERING_DREAM, PlayerSleepStatus.DREAMING);
        allow(PlayerSleepStatus.DREAMING, PlayerSleepStatus.CLOSING_FOR_RETURN);
        allow(PlayerSleepStatus.CLOSING_FOR_RETURN, PlayerSleepStatus.EYES_CLOSED);
        allow(PlayerSleepStatus.EYES_CLOSED, PlayerSleepStatus.DREAMING);
        allow(PlayerSleepStatus.CLOSING_FOR_RETURN, PlayerSleepStatus.RETURNING);
        allow(PlayerSleepStatus.EYES_CLOSED, PlayerSleepStatus.RETURNING);
        allow(PlayerSleepStatus.RETURNING, PlayerSleepStatus.RETURNED_SLEEPING);
        allow(PlayerSleepStatus.RETURNING, PlayerSleepStatus.RETURNED_AWAKE);
        allow(PlayerSleepStatus.RETURNED_SLEEPING, PlayerSleepStatus.OPENING_EYES);
        allow(PlayerSleepStatus.RETURNED_AWAKE, PlayerSleepStatus.OPENING_EYES);
        allow(PlayerSleepStatus.OPENING_EYES, PlayerSleepStatus.COMPLETED);
        allow(PlayerSleepStatus.FAILED, PlayerSleepStatus.RECOVERING);
        allow(PlayerSleepStatus.RECOVERING, PlayerSleepStatus.RETURNED_AWAKE);
    }

    public boolean canMove(PlayerSleepStatus from, PlayerSleepStatus to) {
        return allowed.getOrDefault(Objects.requireNonNull(from, "from"), Set.of())
                .contains(Objects.requireNonNull(to, "to"));
    }

    public PlayerSleepStatus move(PlayerSleepStatus from, PlayerSleepStatus to) {
        if (!canMove(from, to)) {
            throw new IllegalStateException("Invalid sleep transition: " + from + " -> " + to);
        }
        return to;
    }

    private void allow(PlayerSleepStatus from, PlayerSleepStatus to) {
        allowed.computeIfAbsent(from, ignored -> EnumSet.noneOf(PlayerSleepStatus.class)).add(to);
    }
}
