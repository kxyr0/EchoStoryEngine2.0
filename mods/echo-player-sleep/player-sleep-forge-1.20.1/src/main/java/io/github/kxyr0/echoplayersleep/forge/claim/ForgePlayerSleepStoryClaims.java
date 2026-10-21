package io.github.kxyr0.echoplayersleep.forge.claim;

import io.github.kxyr0.echoplayersleep.api.claim.PlayerSleepStoryClaim;
import io.github.kxyr0.echoplayersleep.api.diagnostic.PlayerSleepDiagnostic;
import io.github.kxyr0.echoplayersleep.api.diagnostic.PlayerSleepResult;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepCommandId;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepOwnerId;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepSessionId;
import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepFailure;
import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepOutcome;
import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepStatus;
import io.github.kxyr0.echoplayersleep.api.snapshot.PlayerSleepSessionSnapshot;
import io.github.kxyr0.echoplayersleep.api.world.PlayerRef;
import io.github.kxyr0.echoplayersleep.core.arm.PlayerSleepArmRegistry;
import io.github.kxyr0.echoplayersleep.core.session.InMemoryPlayerSleepSessionRepository;
import io.github.kxyr0.echoplayersleep.core.session.PlayerSleepSessionTransition;
import io.github.kxyr0.echoplayersleep.forge.api.claim.PlayerSleepStoryClaimService;
import io.github.kxyr0.echoplayersleep.forge.api.lifecycle.PlayerSleepLifecyclePhase;
import io.github.kxyr0.echoplayersleep.forge.event.PlayerSleepEventPublisher;
import io.github.kxyr0.echoplayersleep.forge.service.ForgePlayerSleepPlacement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;

public final class ForgePlayerSleepStoryClaims implements PlayerSleepStoryClaimService {
    private final InMemoryPlayerSleepSessionRepository sessions;
    private final PlayerSleepSessionTransition transition;
    private final PlayerSleepArmRegistry arms;
    private final List<PlayerSleepDiagnostic> diagnostics;
    private final PlayerSleepEventPublisher publisher;
    private final ForgePlayerSleepPlacement placement;
    private final Map<PlayerSleepSessionId, PlayerSleepStoryClaim> claims = new LinkedHashMap<>();

    public ForgePlayerSleepStoryClaims(
            Supplier<MinecraftServer> server,
            InMemoryPlayerSleepSessionRepository sessions,
            PlayerSleepSessionTransition transition,
            PlayerSleepArmRegistry arms,
            List<PlayerSleepDiagnostic> diagnostics,
            PlayerSleepEventPublisher publisher) {
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.transition = Objects.requireNonNull(transition, "transition");
        this.arms = Objects.requireNonNull(arms, "arms");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.placement = new ForgePlayerSleepPlacement(server);
    }

    @Override
    public PlayerSleepResult claim(
            PlayerSleepSessionId sessionId,
            String triggerSubscriptionId,
            String triggerExecutionId,
            String storyExecutionId,
            PlayerSleepOwnerId rootOwner,
            PlayerSleepCommandId commandId,
            long serverTick) {
        Objects.requireNonNull(commandId, "commandId");
        Optional<PlayerSleepStoryClaim> previous = claimFor(sessionId);
        if (previous.isPresent()) {
            return handleExistingClaim(previous.orElseThrow(), triggerSubscriptionId,
                    triggerExecutionId, storyExecutionId, rootOwner);
        }
        Optional<PlayerSleepSessionSnapshot> current = sessions.find(sessionId);
        if (current.isEmpty() || current.orElseThrow().status() != PlayerSleepStatus.WAITING_FOR_STORY_CLAIM) {
            return fail(PlayerSleepFailure.PLAYER_SLEEP_STORY_CLAIM_STALE, sessionId.value(),
                    "Sleep session is not waiting for a Story claim.");
        }
        PlayerSleepSessionSnapshot claimed = transition.move(current.orElseThrow(), PlayerSleepStatus.STORY_CLAIMED);
        PlayerSleepStoryClaim claim = new PlayerSleepStoryClaim(sessionId, triggerSubscriptionId,
                triggerExecutionId, storyExecutionId, rootOwner, claimed.revision(), serverTick);
        sessions.save(claimed);
        claims.put(sessionId, claim);
        arms.consume(claimed.player());
        publisher.publishLifecycle(PlayerSleepLifecyclePhase.STORY_CLAIMED, claimed);
        return PlayerSleepResult.success(PlayerSleepOutcome.COMPLETED);
    }

    @Override
    public PlayerSleepResult failUnclaimed(
            PlayerSleepSessionId sessionId,
            PlayerSleepFailure code,
            String message) {
        Optional<PlayerSleepSessionSnapshot> current = sessions.find(sessionId);
        if (current.isEmpty()) {
            return fail(PlayerSleepFailure.PLAYER_SLEEP_STORY_CLAIM_STALE, sessionId.value(),
                    "Sleep session is unavailable.");
        }
        PlayerSleepSessionSnapshot snapshot = current.orElseThrow();
        if (snapshot.status() != PlayerSleepStatus.WAITING_FOR_STORY_CLAIM) {
            return fail(PlayerSleepFailure.PLAYER_SLEEP_STORY_CLAIM_STALE, sessionId.value(),
                    "Sleep session is no longer waiting for a Story claim.");
        }
        wakeSafely(snapshot);
        PlayerSleepSessionSnapshot cancelled = transition.force(transition.bedPose(snapshot, false),
                PlayerSleepStatus.CANCELLED);
        sessions.save(cancelled);
        publisher.publishLifecycle(PlayerSleepLifecyclePhase.CANCELLED, cancelled);
        return fail(code, sessionId.value(), message);
    }

    @Override
    public Optional<PlayerSleepStoryClaim> claimFor(PlayerSleepSessionId sessionId) {
        return Optional.ofNullable(claims.get(Objects.requireNonNull(sessionId, "sessionId")));
    }

    public int clearFor(PlayerRef player) {
        Objects.requireNonNull(player, "player");
        int before = claims.size();
        claims.entrySet().removeIf(entry -> sessions.find(entry.getKey())
                .map(snapshot -> snapshot.player().equals(player))
                .orElse(true));
        return before - claims.size();
    }

    private boolean sameClaim(
            PlayerSleepStoryClaim previous,
            String triggerSubscriptionId,
            String triggerExecutionId,
            String storyExecutionId) {
        return previous.triggerSubscriptionId().equals(triggerSubscriptionId)
                && previous.triggerExecutionId().equals(triggerExecutionId)
                && previous.storyExecutionId().equals(storyExecutionId);
    }

    private PlayerSleepResult handleExistingClaim(
            PlayerSleepStoryClaim previous,
            String triggerSubscriptionId,
            String triggerExecutionId,
            String storyExecutionId,
            PlayerSleepOwnerId rootOwner) {
        if (!previous.rootOwner().equals(rootOwner)) {
            return fail(PlayerSleepFailure.PLAYER_SLEEP_STORY_CLAIM_OWNER_MISMATCH,
                    previous.sessionId().value(), "Sleep session is claimed by a different root owner.");
        }
        return sameClaim(previous, triggerSubscriptionId, triggerExecutionId, storyExecutionId)
                ? PlayerSleepResult.success(PlayerSleepOutcome.COMPLETED)
                : fail(PlayerSleepFailure.PLAYER_SLEEP_STORY_CLAIM_CONFLICT, previous.sessionId().value(),
                        "Sleep session already has a different Story claim.");
    }

    private void wakeSafely(PlayerSleepSessionSnapshot snapshot) {
        placement.player(snapshot.player()).ifPresent(player -> {
            if (player.isSleeping()) {
                placement.releaseBedPose(player);
            }
        });
    }

    private PlayerSleepResult fail(PlayerSleepFailure code, String subject, String message) {
        PlayerSleepDiagnostic diagnostic = PlayerSleepDiagnostic.error(code, subject, message);
        diagnostics.add(diagnostic);
        return PlayerSleepResult.failure(PlayerSleepOutcome.INTERNAL_FAILURE, diagnostic);
    }
}
