package io.github.kxyr0.echoscene.forge.command;

import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneCommandId;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneId;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneEndBehavior;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneOutcome;
import io.github.kxyr0.echocutscene.forge.api.event.CutsceneServiceLookupEvent;
import io.github.kxyr0.echocutscene.forge.api.service.CutsceneService;
import io.github.kxyr0.echoscene.api.command.SceneCommandRequest;
import io.github.kxyr0.echoscene.api.command.SceneCommandStart;
import io.github.kxyr0.echoscene.api.identity.SceneExecutionId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;

final class CameraCommandAdapter {
    private final Function<SceneExecutionId, ServerPlayer> players;
    private final Map<SceneExecutionId, Active> active = new LinkedHashMap<>();

    CameraCommandAdapter(Function<SceneExecutionId, ServerPlayer> players) { this.players = players; }

    SceneCommandStart start(SceneCommandRequest request) {
        ServerPlayer player = players.apply(request.executionId());
        CutsceneService service = service(player).orElse(null);
        if (service == null || player == null) return SceneCommandStart.failed("Echo Cutscene service unavailable");
        CutsceneId id = CutsceneId.parse(request.command().arguments().get(0));
        CutsceneCommandId command = new CutsceneCommandId(SceneCommandHub.key(request));
        io.github.kxyr0.echoscene.forge.command.context.SceneEventContexts.capture(request, player, command.value());
        var result = service.start(player, id, command, CutsceneEndBehavior.HOLD_LAST_FRAME);
        if (result.outcome() != CutsceneOutcome.ACCEPTED
                && result.outcome() != CutsceneOutcome.ALREADY_PLAYING) {
            return SceneCommandStart.failed(result.diagnostics().isEmpty() ? result.outcome().name()
                    : result.diagnostics().get(0).message());
        }
        active.put(request.executionId(), new Active(player, id, command));
        return SceneCommandStart.completed();
    }

    void restoreOwned(SceneExecutionId owner) {
        Active value = active.remove(owner);
        if (value == null) return;
        service(value.player()).ifPresent(service -> service.stop(value.player(), value.cutscene(),
                new CutsceneCommandId(value.command().value() + "-restore")));
    }

    private Optional<CutsceneService> service(ServerPlayer player) {
        if (player == null) return Optional.empty();
        CutsceneServiceLookupEvent event = new CutsceneServiceLookupEvent(player.getServer());
        MinecraftForge.EVENT_BUS.post(event);
        return event.providers().size() == 1 ? Optional.of(event.providers().get(0)) : Optional.empty();
    }

    private record Active(ServerPlayer player, CutsceneId cutscene, CutsceneCommandId command) { }
}
