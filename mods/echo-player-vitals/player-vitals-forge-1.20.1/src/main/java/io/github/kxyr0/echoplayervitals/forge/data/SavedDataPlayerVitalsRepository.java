package io.github.kxyr0.echoplayervitals.forge.data;

import io.github.kxyr0.echoplayervitals.api.identity.PlayerVitalsId;
import io.github.kxyr0.echoplayervitals.api.snapshot.PlayerVitalsSnapshot;
import io.github.kxyr0.echoplayervitals.core.config.PlayerVitalsConfig;
import io.github.kxyr0.echoplayervitals.core.persistence.PlayerVitalsData;
import io.github.kxyr0.echoplayervitals.core.persistence.PlayerVitalsPersistenceCodec;
import io.github.kxyr0.echoplayervitals.core.repository.PlayerVitalsRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;

/**
 * SavedData-backed repository for restart-durable vitals.
 */
public final class SavedDataPlayerVitalsRepository implements PlayerVitalsRepository {
    private final PlayerVitalsSavedData savedData;
    private final PlayerVitalsPersistenceCodec codec = new PlayerVitalsPersistenceCodec();
    private final Map<PlayerVitalsId, PlayerVitalsSnapshot> snapshots = new LinkedHashMap<>();

    private SavedDataPlayerVitalsRepository(PlayerVitalsSavedData savedData) {
        this.savedData = savedData;
        load();
    }

    public static SavedDataPlayerVitalsRepository open(MinecraftServer server) {
        PlayerVitalsSavedData saved = server.overworld().getDataStorage().computeIfAbsent(
                PlayerVitalsSavedData::load, PlayerVitalsSavedData::new, PlayerVitalsSavedData.NAME);
        return new SavedDataPlayerVitalsRepository(saved);
    }

    @Override
    public synchronized Optional<PlayerVitalsSnapshot> find(PlayerVitalsId playerId) {
        return Optional.ofNullable(snapshots.get(playerId));
    }

    @Override
    public synchronized List<PlayerVitalsSnapshot> findAll() {
        return List.copyOf(snapshots.values());
    }

    @Override
    public synchronized PlayerVitalsSnapshot save(PlayerVitalsSnapshot snapshot) {
        snapshots.put(snapshot.playerId(), snapshot);
        flush();
        return snapshot;
    }

    private void load() {
        PlayerVitalsConfig config = PlayerVitalsConfig.defaults();
        for (String encoded : savedData.encodedProfiles()) {
            PlayerVitalsSnapshot snapshot = codec.decode(encoded).toSnapshot(config);
            snapshots.put(snapshot.playerId(), snapshot);
        }
    }

    private void flush() {
        savedData.replaceAll(snapshots.values().stream()
                .map(PlayerVitalsData::fromSnapshot)
                .map(codec::encode)
                .toList());
    }
}
