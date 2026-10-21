package io.github.kxyr0.echoscene.forge.api;

import io.github.kxyr0.echoscene.api.identity.SceneExecutionId;
import io.github.kxyr0.echoscene.api.model.SceneInspection;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

public final class EchoSceneCancelEvent extends Event {
    private final MinecraftServer server;
    private final SceneExecutionId executionId;
    private SceneInspection result;

    public EchoSceneCancelEvent(MinecraftServer server, SceneExecutionId executionId) {
        this.server = Objects.requireNonNull(server, "server");
        this.executionId = Objects.requireNonNull(executionId, "executionId");
    }

    public MinecraftServer server() { return server; }
    public SceneExecutionId executionId() { return executionId; }
    public void provide(SceneInspection inspection) { result = Objects.requireNonNull(inspection, "inspection"); }
    public Optional<SceneInspection> result() { return Optional.ofNullable(result); }
}
