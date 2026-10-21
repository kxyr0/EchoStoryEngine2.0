package io.github.kxyr0.echoplayer.forge;

import io.github.kxyr0.echoplayer.core.lifecycle.PlayerLifecycleCheckpoint;
import io.github.kxyr0.echoplayer.core.mutation.PlayerMutationLedger;
import io.github.kxyr0.echoplayer.core.repository.InMemoryPlayerProfileRepository;
import io.github.kxyr0.echoplayer.core.repository.PlayerProfileRepository;
import io.github.kxyr0.echoplayer.core.template.PlayerProfileTemplate;
import io.github.kxyr0.echoplayer.forge.log.PlayerStructuredLogger;
import io.github.kxyr0.echoplayer.forge.service.ForgePlayerProfileServices;
import java.util.function.Consumer;
import io.github.kxyr0.echoplayer.api.profile.PlayerProfileSnapshot;
import java.util.Objects;

/**
 * Runtime object graph owned by the Forge mod instance.
 */
public final class ForgePlayerRuntimeState {
    private PlayerProfileRepository repository;
    private final PlayerLifecycleCheckpoint lifecycle;
    private final PlayerMutationLedger mutationLedger;
    private final PlayerProfileTemplate defaultTemplate;
    private final ForgePlayerProfileServices services;

    private ForgePlayerRuntimeState(PlayerStructuredLogger logger, Consumer<PlayerProfileSnapshot> publisher) {
        this.repository = new InMemoryPlayerProfileRepository();
        this.lifecycle = new PlayerLifecycleCheckpoint();
        this.mutationLedger = new PlayerMutationLedger();
        this.defaultTemplate = PlayerProfileTemplate.fallback();
        this.services = new ForgePlayerProfileServices(this::repository, mutationLedger, defaultTemplate, logger,
                publisher);
    }

    public static ForgePlayerRuntimeState create(PlayerStructuredLogger logger,
                                                 Consumer<PlayerProfileSnapshot> publisher) {
        return new ForgePlayerRuntimeState(Objects.requireNonNull(logger, "logger"),
                Objects.requireNonNull(publisher, "publisher"));
    }

    public synchronized PlayerProfileRepository repository() {
        return repository;
    }

    public synchronized void useRepository(PlayerProfileRepository nextRepository) {
        this.repository = Objects.requireNonNull(nextRepository, "nextRepository");
    }

    public PlayerLifecycleCheckpoint lifecycle() {
        return lifecycle;
    }

    public PlayerProfileTemplate defaultTemplate() {
        return defaultTemplate;
    }

    public ForgePlayerProfileServices services() {
        return services;
    }
}
