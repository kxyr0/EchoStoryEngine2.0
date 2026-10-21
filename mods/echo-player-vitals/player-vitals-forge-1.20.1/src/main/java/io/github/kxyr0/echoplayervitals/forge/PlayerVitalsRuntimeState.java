package io.github.kxyr0.echoplayervitals.forge;

import io.github.kxyr0.echoplayervitals.core.config.PlayerVitalsConfig;
import io.github.kxyr0.echoplayervitals.core.mutation.PlayerVitalsMutationLedger;
import io.github.kxyr0.echoplayervitals.core.repository.InMemoryPlayerVitalsRepository;
import io.github.kxyr0.echoplayervitals.core.repository.PlayerVitalsRepository;
import io.github.kxyr0.echoplayervitals.forge.service.ForgePlayerVitalsServices;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Runtime object graph owned by the Forge mod instance.
 */
public final class PlayerVitalsRuntimeState {
    private PlayerVitalsRepository repository;
    private final ForgePlayerVitalsServices services;

    private PlayerVitalsRuntimeState(Consumer<PlayerVitalsPublication> publisher) {
        this.repository = new InMemoryPlayerVitalsRepository();
        this.services = new ForgePlayerVitalsServices(this::repository, PlayerVitalsConfig.defaults(),
                new PlayerVitalsMutationLedger(), publisher);
    }

    public static PlayerVitalsRuntimeState create(Consumer<PlayerVitalsPublication> publisher) {
        return new PlayerVitalsRuntimeState(Objects.requireNonNull(publisher, "publisher"));
    }

    public synchronized PlayerVitalsRepository repository() {
        return repository;
    }

    public synchronized void useRepository(PlayerVitalsRepository nextRepository) {
        this.repository = Objects.requireNonNull(nextRepository, "nextRepository");
    }

    public ForgePlayerVitalsServices services() {
        return services;
    }
}
