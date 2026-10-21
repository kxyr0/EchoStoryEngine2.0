package io.github.kxyr0.echoscene.forge.command;

import io.github.kxyr0.echoscene.api.command.SceneCommandPort;
import io.github.kxyr0.echoscene.forge.audio.AudioCommandAdapter;
import io.github.kxyr0.echoscene.api.command.SceneCommandRequest;
import io.github.kxyr0.echoscene.api.command.SceneCommandStart;
import io.github.kxyr0.echoscene.api.definition.SceneCapabilityKind;
import io.github.kxyr0.echoscene.api.definition.SceneDefinition;
import io.github.kxyr0.echoscene.api.identity.SceneExecutionId;
import io.github.kxyr0.echoscene.api.identity.SceneStepId;
import io.github.kxyr0.echoscene.api.model.SceneInspection;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.server.level.ServerPlayer;

public final class SceneCommandHub implements SceneCommandPort {
    private final TimeCommandAdapter time;
    private final DialogueCommandAdapter dialogue;
    private final MovementCommandAdapter movement;
    private final AnimationCommandAdapter animation;
    private final CameraCommandAdapter camera;
    private final NestedSceneCommandAdapter nested;
    private final AudioCommandAdapter audio;
    private final Function<SceneExecutionId, ServerPlayer> players;
    private final MovementCommandAdapter.LocationCatalog locations;
    private Completion completion = (execution, step, succeeded, detail) -> { };

    public SceneCommandHub(Function<SceneExecutionId, ServerPlayer> players,
            MovementCommandAdapter.LocationCatalog locations) {
        this.players = players;
        this.locations = locations;
        time = new TimeCommandAdapter(players);
        dialogue = new DialogueCommandAdapter(players);
        movement = new MovementCommandAdapter(players, locations);
        animation = new AnimationCommandAdapter(players);
        camera = new CameraCommandAdapter(players);
        nested = new NestedSceneCommandAdapter(players);
        audio = new AudioCommandAdapter(players, locations);
    }

    public void bind(Completion value, NestedScenes scenes) {
        completion = value;
        nested.bind(scenes);
    }

    @Override
    public SceneCommandStart start(SceneCommandRequest request) {
        try {
            String receiver = request.command().receiver();
            if (receiver.equals("Sound")) return audio.start(request, completion);
            if (receiver.equals("Time")) return time.start(request);
            if (receiver.equals("Dialogue")) return dialogue.start(request);
            if (receiver.equals("Camera")) return camera.start(request);
            if (receiver.equals("Scene")) return nested.start(request);
            if (request.command().operation().equals("MoveTo")) return movement.start(request);
            if (request.command().operation().equals("Play")) return animation.start(request);
            var external = new io.github.kxyr0.echoscene.forge.api.SceneExternalCommandEvent(
                    request, players.apply(request.executionId()), locations, completion);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(external);
            if (external.result().isPresent()) return external.result().orElseThrow();
            return SceneCommandStart.failed("no Forge adapter for " + receiver + "."
                    + request.command().operation());
        } catch (RuntimeException exception) {
            return SceneCommandStart.failed("Forge adapter rejected command: " + exception.getMessage());
        }
    }

    public void tick() {
        time.tick(completion);
        dialogue.tick(completion);
        movement.tick(completion);
        animation.tick(completion);
        nested.tick(completion);
    }

    @Override
    public void cancelOwned(SceneExecutionId owner) {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                new io.github.kxyr0.echoscene.forge.api.SceneExternalCancelEvent(owner));
        audio.cancelOwned(owner);
        time.cancelOwned(owner);
        dialogue.cancelOwned(owner);
        movement.cancelOwned(owner);
        animation.cancelOwned(owner);
        nested.cancelOwned(owner);
    }

    public void restoreCamera(SceneExecutionId owner) { camera.restoreOwned(owner); audio.cancelOwned(owner); }

    static boolean playerCommand(SceneCommandRequest request) {
        return request.command().capabilities().stream()
                .anyMatch(value -> value.kind() == SceneCapabilityKind.PLAYER);
    }

    static String key(SceneCommandRequest request) {
        return "echo_scene:" + token(request.executionId().value()) + "/" + request.command().stepId().value();
    }

    public static String token(String value) {
        return value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
    }

    @FunctionalInterface
    public interface Completion {
        void complete(SceneExecutionId execution, SceneStepId step, boolean succeeded, String detail);
    }

    public interface NestedScenes {
        Optional<SceneDefinition> find(String resourceId);
        SceneInspection start(SceneDefinition definition, ServerPlayer player, String rootOwner);
        Optional<SceneInspection> inspect(SceneExecutionId executionId);
        void cancel(SceneExecutionId executionId);
    }
}
