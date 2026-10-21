package io.github.kxyr0.echoscene.forge.command;

import io.github.kxyr0.echoscene.api.command.SceneCommandRequest;
import io.github.kxyr0.echoscene.api.command.SceneCommandStart;
import io.github.kxyr0.echoscene.api.identity.SceneExecutionId;
import io.github.kxyr0.echoscene.api.identity.SceneStepId;
import io.github.kxyr0.echoscene.api.model.SceneExecutionState;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.server.level.ServerPlayer;

final class NestedSceneCommandAdapter {
    private final Function<SceneExecutionId, ServerPlayer> players;
    private final Map<SceneExecutionId, Pending> pending = new LinkedHashMap<>();
    private SceneCommandHub.NestedScenes scenes;

    NestedSceneCommandAdapter(Function<SceneExecutionId, ServerPlayer> players) { this.players = players; }

    void bind(SceneCommandHub.NestedScenes value) { scenes = value; }

    SceneCommandStart start(SceneCommandRequest request) {
        if (scenes == null) return SceneCommandStart.failed("Scene catalog unavailable");
        String resource = request.command().arguments().get(0);
        if (resource.equals(request.resourceId().value())) {
            return SceneCommandStart.failed("SCENE_RECURSION_DETECTED: " + resource);
        }
        ServerPlayer player = players.apply(request.executionId());
        if (player == null) return SceneCommandStart.failed("player unavailable");
        var definition = scenes.find(resource).orElse(null);
        if (definition == null) return SceneCommandStart.failed("SCENE_RESOURCE_NOT_FOUND: " + resource);
        var child = scenes.start(definition, player, request.rootOwner());
        if (child.state() == SceneExecutionState.COMPLETED) return SceneCommandStart.completed();
        if (child.state().terminal()) return SceneCommandStart.failed(child.failure().orElse(child.state().name()));
        pending.put(child.executionId(), new Pending(request.executionId(), request.command().stepId(),
                child.executionId()));
        return SceneCommandStart.waiting(child.executionId().value());
    }

    void tick(SceneCommandHub.Completion completion) {
        for (Pending value : List.copyOf(pending.values())) {
            var inspection = scenes.inspect(value.child()).orElse(null);
            if (inspection == null || !inspection.state().terminal()) continue;
            pending.remove(value.child());
            boolean succeeded = inspection.state() == SceneExecutionState.COMPLETED;
            completion.complete(value.parent(), value.step(), succeeded,
                    succeeded ? "" : inspection.failure().orElse(inspection.state().name()));
        }
    }

    void cancelOwned(SceneExecutionId owner) {
        pending.values().removeIf(value -> {
            if (!value.parent().equals(owner)) return false;
            scenes.cancel(value.child());
            return true;
        });
    }

    private record Pending(SceneExecutionId parent, SceneStepId step, SceneExecutionId child) { }
}
