package io.github.kxyr0.echonpcbackpack.forge.runtime;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.forge.api.navigation.EchoNpcNavigationBodyBridge;
import io.github.kxyr0.echonpc.forge.api.navigation.NpcNavigationBody;
import io.github.kxyr0.echonpcbackpack.api.view.NpcBackpackDiagnostic;
import io.github.kxyr0.echonpcbackpack.api.world.BackpackBlockLocation;
import io.github.kxyr0.echonpcbackpack.forge.approach.BackpackAlignment;
import io.github.kxyr0.echonpcbackpack.forge.approach.BackpackApproachCoordinator;
import io.github.kxyr0.echonpcbackpack.forge.network.BackpackNetwork;
import io.github.kxyr0.echonpcbackpack.forge.sophisticated.SophisticatedBackpackAccess;
import io.github.kxyr0.echonpcbackpack.forge.state.BackpackActorRecord;
import io.github.kxyr0.echonpcbackpack.forge.state.BackpackJournalSnapshot;
import io.github.kxyr0.echonpcbackpack.forge.state.BackpackJournalStore;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

public final class BackpackRuntimeState {
    public final Map<String, BackpackActorRecord> actors = new LinkedHashMap<>();
    public final Map<String, String> terminalCommands = new LinkedHashMap<>();
    public final SophisticatedBackpackAccess access = new SophisticatedBackpackAccess();
    public final BackpackWorldMutations mutations = new BackpackWorldMutations(access);
    public final BackpackViewMapper views = new BackpackViewMapper(access);
    public final BackpackApproachCoordinator approach = new BackpackApproachCoordinator();
    public final BackpackAlignment alignment = new BackpackAlignment();
    private final Deque<NpcBackpackDiagnostic> diagnostics = new ArrayDeque<>();
    private MinecraftServer server;
    private BackpackJournalStore store;
    private long repositoryRevision = 1L;

    public void start(MinecraftServer value) throws IOException {
        server = java.util.Objects.requireNonNull(value, "server");
        store = new BackpackJournalStore(server.getWorldPath(LevelResource.ROOT));
        BackpackJournalSnapshot snapshot = store.load();
        actors.clear();
        actors.putAll(snapshot.actors());
        terminalCommands.clear();
        terminalCommands.putAll(snapshot.terminalCommands());
        repositoryRevision = snapshot.repositoryRevision();
    }

    public void stop() {
        server = null;
        store = null;
        actors.clear();
        terminalCommands.clear();
        diagnostics.clear();
    }

    public void persist() throws IOException {
        if (store == null) throw new IOException("Backpack journal is unavailable.");
        while (terminalCommands.size() > 1024) {
            terminalCommands.remove(terminalCommands.keySet().iterator().next());
        }
        repositoryRevision++;
        store.save(new BackpackJournalSnapshot(actors, terminalCommands, repositoryRevision));
        if (server != null) actors.values().forEach(record -> BackpackNetwork.publish(server, record));
    }

    public MinecraftServer server() {
        if (server == null) throw new IllegalStateException("Backpack runtime is stopped.");
        return server;
    }

    public boolean ready() {
        return server != null && store != null;
    }

    public Optional<ServerLevel> level(BackpackBlockLocation location) {
        ResourceLocation id = ResourceLocation.tryParse(location.dimension().value());
        if (id == null || server == null) return Optional.empty();
        return Optional.ofNullable(server.getLevel(ResourceKey.create(Registries.DIMENSION, id)));
    }

    public Optional<NpcNavigationBody> body(ActorId actorId) {
        if (server == null) return Optional.empty();
        return EchoNpcNavigationBodyBridge.lookup(server, actorId).body();
    }

    public boolean reservedByOther(ActorId actor, BackpackBlockLocation location) {
        return actors.values().stream().filter(BackpackActorRecord::transitioning)
                .filter(record -> !record.actorId.equals(actor))
                .anyMatch(record -> record.source.filter(location::equals).isPresent()
                        || record.destination.filter(location::equals).isPresent());
    }

    public boolean payloadOwnedByOther(ActorId actor, UUID contentsUuid) {
        return actors.values().stream().filter(record -> !record.actorId.equals(actor))
                .filter(BackpackActorRecord::carryingPayload)
                .map(record -> access.contentsUuid(record.payload))
                .flatMap(Optional::stream).anyMatch(contentsUuid::equals);
    }

    public void diagnostic(NpcBackpackDiagnostic value) {
        diagnostics.addLast(value);
        while (diagnostics.size() > 100) diagnostics.removeFirst();
    }

    public List<NpcBackpackDiagnostic> diagnostics() {
        return List.copyOf(diagnostics);
    }

    public long gameTick() {
        ServerLevel overworld = server == null ? null : server.getLevel(Level.OVERWORLD);
        return overworld == null ? 0L : overworld.getGameTime();
    }
}
