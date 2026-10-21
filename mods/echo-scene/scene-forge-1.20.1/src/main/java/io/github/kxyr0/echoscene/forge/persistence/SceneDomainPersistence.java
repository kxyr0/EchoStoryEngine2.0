package io.github.kxyr0.echoscene.forge.persistence;

import io.github.kxyr0.echoscene.api.definition.SceneDefinition;
import io.github.kxyr0.echoscene.api.identity.SceneExecutionId;
import io.github.kxyr0.echoscene.persistence.SavedScene;
import io.github.kxyr0.echoscene.persistence.SceneFileStore;
import io.github.kxyr0.echoscene.runtime.SceneCheckpoint;
import io.github.kxyr0.echoscene.runtime.SceneRuntime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

public final class SceneDomainPersistence {
    private final Map<SceneExecutionId, SavedScene> saved = new LinkedHashMap<>();
    private final Map<SceneExecutionId, SceneDefinition> definitions = new LinkedHashMap<>();
    private final Map<SceneExecutionId, UUID> players = new LinkedHashMap<>();
    private SceneFileStore store;
    private MinecraftServer server;

    public void initialize(MinecraftServer server, SceneRuntime runtime) {
        saved.clear();
        definitions.clear();
        players.clear();
        this.server = server;
        store = new SceneFileStore(server.getWorldPath(LevelResource.ROOT).resolve("echo/scenes/executions"));
        for (var scene : store.loadAll()) {
            saved.put(scene.checkpoint().executionId(), scene);
            register(scene.definition(), scene.checkpoint().executionId(), scene.player());
            if (scene.checkpoint().state().terminal()) {
                runtime.recover(scene.definition(), scene.checkpoint());
                SceneEventOutbox.publish(server, scene, true);
            }
        }
    }
    public void register(SceneDefinition definition, SceneExecutionId id, UUID player) {
        definitions.put(id, definition);
        players.put(id, player);
    }
    public Optional<SavedScene> find(SceneExecutionId id) { return Optional.ofNullable(saved.get(id)); }
    public void recoverPlayer(ServerPlayer player, SceneRuntime runtime, Map<SceneExecutionId, ServerPlayer> activePlayers) {
        for (var scene : java.util.List.copyOf(saved.values())) {
            var id = scene.checkpoint().executionId();
            if (!scene.player().equals(player.getUUID()) || scene.checkpoint().state().terminal()
                    || runtime.inspect(id).isPresent()) continue;
            activePlayers.put(id, player);
            runtime.recover(scene.definition(), scene.checkpoint());
        }
    }
    public void save(SceneCheckpoint checkpoint) {
        if (store == null) throw new IllegalStateException("Scene persistence is not initialized");
        var scene = new SavedScene(definitions.get(checkpoint.executionId()), checkpoint,
                players.get(checkpoint.executionId()), server.getTickCount());
        scene = SceneEventOutbox.pin(server, scene);
        store.save(scene);
        saved.put(checkpoint.executionId(), scene);
        SceneEventOutbox.publish(server, scene, false);
    }
}
