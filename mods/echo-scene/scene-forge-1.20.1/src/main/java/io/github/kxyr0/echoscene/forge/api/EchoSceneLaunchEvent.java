package io.github.kxyr0.echoscene.forge.api;

import io.github.kxyr0.echoscene.api.model.SceneInspection;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

public final class EchoSceneLaunchEvent extends Event {
    private final ServerPlayer player;
    private final String resourceId;
    private SceneInspection result;
    private final Optional<io.github.kxyr0.echoscene.api.identity.SceneExecutionId> execution;
    private final Optional<String> rootExecution;

    public EchoSceneLaunchEvent(ServerPlayer player, String resourceId,
            io.github.kxyr0.echoscene.api.identity.SceneExecutionId execution, String rootExecution) {
        this.player = Objects.requireNonNull(player, "player");
        this.resourceId = Objects.requireNonNull(resourceId, "resourceId");
        this.execution = Optional.of(execution);
        this.rootExecution = Optional.of(rootExecution);
    }
    public Optional<io.github.kxyr0.echoscene.api.identity.SceneExecutionId> execution() { return execution; }
    public Optional<String> rootExecution() { return rootExecution; }

    public EchoSceneLaunchEvent(ServerPlayer player, String resourceId) {
        this.player = Objects.requireNonNull(player, "player");
        this.resourceId = Objects.requireNonNull(resourceId, "resourceId");
        this.execution = Optional.empty();
        this.rootExecution = Optional.empty();
    }

    public ServerPlayer player() { return player; }
    public String resourceId() { return resourceId; }
    public void provide(SceneInspection inspection) { result = Objects.requireNonNull(inspection, "inspection"); }
    public Optional<SceneInspection> result() { return Optional.ofNullable(result); }
}
