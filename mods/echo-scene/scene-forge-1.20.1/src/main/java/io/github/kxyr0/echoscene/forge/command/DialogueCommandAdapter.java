package io.github.kxyr0.echoscene.forge.command;

import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.service.DialogueOperationStatus;
import io.github.kxyr0.echodialogue.api.service.DialogueService;
import io.github.kxyr0.echodialogue.api.service.DialogueStartRequest;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionStatus;
import io.github.kxyr0.echodialogue.forge.api.EchoDialogueServiceLookupEvent;
import io.github.kxyr0.echoscene.api.command.SceneCommandRequest;
import io.github.kxyr0.echoscene.api.command.SceneCommandStart;
import io.github.kxyr0.echoscene.api.identity.SceneExecutionId;
import io.github.kxyr0.echoscene.api.identity.SceneStepId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;

final class DialogueCommandAdapter {
    private final Function<SceneExecutionId, ServerPlayer> players;
    private final Map<String, Pending> pending = new LinkedHashMap<>();

    DialogueCommandAdapter(Function<SceneExecutionId, ServerPlayer> players) { this.players = players; }

    SceneCommandStart start(SceneCommandRequest request) {
        ServerPlayer player = players.apply(request.executionId());
        DialogueService service = service(player).orElse(null);
        if (service == null || player == null) return SceneCommandStart.failed("Echo Dialogue service unavailable");
        DialogueSessionId session = new DialogueSessionId(SceneCommandHub.key(request));
        var result = service.start(new DialogueStartRequest(session,
                DialogueId.parse(request.command().arguments().get(0)), new AudienceId(player.getUUID().toString()),
                Optional.of(new DialogueStartRequest.Origin(request.rootOwner(), request.executionId().value()))));
        if (!accepted(result.status())) return SceneCommandStart.failed(message(result.status(), result.diagnostics()));
        if (request.command().operation().equals("Start")) return SceneCommandStart.completed();
        Optional<DialogueSessionStatus> status = result.session().map(value -> value.status());
        if (status.filter(value -> value == DialogueSessionStatus.COMPLETED).isPresent()) {
            return SceneCommandStart.completed();
        }
        if (status.filter(this::failed).isPresent()) return SceneCommandStart.failed("Dialogue is terminal: "
                + status.orElseThrow());
        pending.put(session.value(), new Pending(request.executionId(), request.command().stepId(), session, player));
        return SceneCommandStart.waiting(session.value());
    }

    void tick(SceneCommandHub.Completion completion) {
        for (Pending value : List.copyOf(pending.values())) {
            DialogueService service = service(value.player()).orElse(null);
            if (service == null) continue;
            var result = service.query(value.session());
            Optional<DialogueSessionStatus> status = result.session().map(session -> session.status());
            if (status.filter(current -> current == DialogueSessionStatus.COMPLETED).isPresent()) {
                finish(value, true, "", completion);
            } else if (status.filter(this::failed).isPresent()
                    || result.status() == DialogueOperationStatus.NOT_FOUND
                    || result.status() == DialogueOperationStatus.FAILED) {
                finish(value, false, "Dialogue ended with " + status.map(Enum::name)
                        .orElse(result.status().name()), completion);
            }
        }
    }

    void cancelOwned(SceneExecutionId owner) {
        pending.values().removeIf(value -> {
            if (!value.execution().equals(owner)) return false;
            service(value.player()).ifPresent(service -> service.cancel(value.session()));
            return true;
        });
    }

    private void finish(Pending value, boolean succeeded, String detail, SceneCommandHub.Completion completion) {
        pending.remove(value.session().value());
        completion.complete(value.execution(), value.step(), succeeded, detail);
    }

    private Optional<DialogueService> service(ServerPlayer player) {
        if (player == null) return Optional.empty();
        EchoDialogueServiceLookupEvent event = new EchoDialogueServiceLookupEvent(player.getServer());
        MinecraftForge.EVENT_BUS.post(event);
        return event.providers().size() == 1 ? Optional.of(event.providers().get(0)) : Optional.empty();
    }

    private boolean accepted(DialogueOperationStatus status) {
        return status == DialogueOperationStatus.ACCEPTED || status == DialogueOperationStatus.STARTED
                || status == DialogueOperationStatus.ALREADY_ACTIVE || status == DialogueOperationStatus.WAITING
                || status == DialogueOperationStatus.ALREADY_TERMINAL;
    }

    private boolean failed(DialogueSessionStatus status) {
        return status == DialogueSessionStatus.CANCELLED || status == DialogueSessionStatus.FAILED
                || status == DialogueSessionStatus.BLOCKED;
    }

    private String message(DialogueOperationStatus status, List<?> diagnostics) {
        return "Dialogue start " + status + (diagnostics.isEmpty() ? "" : ": " + diagnostics.get(0));
    }

    private record Pending(SceneExecutionId execution, SceneStepId step, DialogueSessionId session,
                           ServerPlayer player) { }
}
