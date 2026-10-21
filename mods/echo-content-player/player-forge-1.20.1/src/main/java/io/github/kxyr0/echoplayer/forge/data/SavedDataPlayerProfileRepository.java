package io.github.kxyr0.echoplayer.forge.data;

import io.github.kxyr0.echoplayer.api.identity.EchoPlayerId;
import io.github.kxyr0.echoplayer.api.profile.PlayerProfileSnapshot;
import io.github.kxyr0.echoplayer.core.persistence.PlayerPersistenceCodec;
import io.github.kxyr0.echoplayer.core.persistence.PlayerProfileData;
import io.github.kxyr0.echoplayer.core.repository.PlayerProfileRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;

/**
 * SavedData-backed repository for restart-durable server profiles.
 */
public final class SavedDataPlayerProfileRepository implements PlayerProfileRepository {
    private final PlayerProfileSavedData savedData;
    private final PlayerPersistenceCodec codec = new PlayerPersistenceCodec();
    private final Map<EchoPlayerId, PlayerProfileSnapshot> snapshots = new LinkedHashMap<>();
    private long repositoryRevision;

    private SavedDataPlayerProfileRepository(PlayerProfileSavedData savedData) {
        this.savedData = savedData;
        load();
    }

    public static SavedDataPlayerProfileRepository open(MinecraftServer server) {
        PlayerProfileSavedData saved = server.overworld().getDataStorage().computeIfAbsent(
                PlayerProfileSavedData::load, PlayerProfileSavedData::new, PlayerProfileSavedData.NAME);
        return new SavedDataPlayerProfileRepository(saved);
    }

    @Override
    public synchronized Optional<PlayerProfileSnapshot> find(EchoPlayerId playerId) {
        return Optional.ofNullable(snapshots.get(playerId));
    }

    @Override
    public synchronized List<PlayerProfileSnapshot> findAll() {
        return List.copyOf(snapshots.values());
    }

    @Override
    public synchronized PlayerProfileSnapshot save(PlayerProfileSnapshot snapshot) {
        repositoryRevision = Math.max(repositoryRevision + 1L, snapshot.repositoryRevision() + 1L);
        PlayerProfileSnapshot saved = snapshot.withRevision(snapshot.profileRevision(), repositoryRevision);
        snapshots.put(saved.playerId(), saved);
        flush();
        return saved;
    }

    @Override
    public synchronized int size() {
        return snapshots.size();
    }

    private void load() {
        for (String encoded : savedData.encodedProfiles()) {
            PlayerProfileSnapshot snapshot = codec.decode(encoded).toSnapshot();
            snapshots.put(snapshot.playerId(), snapshot);
            repositoryRevision = Math.max(repositoryRevision, snapshot.repositoryRevision());
        }
    }

    private void flush() {
        savedData.replaceAll(snapshots.values().stream()
                .map(PlayerProfileData::fromSnapshot)
                .map(codec::encode)
                .toList());
    }
}
