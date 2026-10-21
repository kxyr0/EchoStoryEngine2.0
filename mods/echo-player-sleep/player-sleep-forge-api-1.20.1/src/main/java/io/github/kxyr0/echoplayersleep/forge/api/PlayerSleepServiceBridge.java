package io.github.kxyr0.echoplayersleep.forge.api;

import io.github.kxyr0.echoplayersleep.forge.api.arm.PlayerSleepArmService;
import io.github.kxyr0.echoplayersleep.forge.api.bed.PlayerSleepBedProviderRegistry;
import io.github.kxyr0.echoplayersleep.forge.api.catalog.PlayerSleepScenarioCatalogView;
import io.github.kxyr0.echoplayersleep.forge.api.claim.PlayerSleepStoryClaimService;
import io.github.kxyr0.echoplayersleep.forge.api.diagnostic.PlayerSleepDiagnosticsView;
import io.github.kxyr0.echoplayersleep.forge.api.recovery.PlayerSleepRecoveryService;
import io.github.kxyr0.echoplayersleep.forge.api.session.PlayerSleepSessionQueryService;
import io.github.kxyr0.echoplayersleep.forge.api.session.PlayerSleepSessionService;
import io.github.kxyr0.echoplayersleep.forge.api.transition.PlayerSleepTransitionClientService;
import java.util.Objects;

public record PlayerSleepServiceBridge(
        PlayerSleepScenarioCatalogView catalog,
        PlayerSleepArmService arms,
        PlayerSleepSessionService sessions,
        PlayerSleepSessionQueryService query,
        PlayerSleepBedProviderRegistry bedProviders,
        PlayerSleepStoryClaimService claims,
        PlayerSleepTransitionClientService transitions,
        PlayerSleepDiagnosticsView diagnostics,
        PlayerSleepRecoveryService recovery) {
    public PlayerSleepServiceBridge {
        catalog = Objects.requireNonNull(catalog, "catalog");
        arms = Objects.requireNonNull(arms, "arms");
        sessions = Objects.requireNonNull(sessions, "sessions");
        query = Objects.requireNonNull(query, "query");
        bedProviders = Objects.requireNonNull(bedProviders, "bedProviders");
        claims = Objects.requireNonNull(claims, "claims");
        transitions = Objects.requireNonNull(transitions, "transitions");
        diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
        recovery = Objects.requireNonNull(recovery, "recovery");
    }
}
