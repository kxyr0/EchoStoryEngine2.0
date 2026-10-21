package io.github.kxyr0.echonpc.forge.command;

import io.github.kxyr0.echonpc.api.appearance.NpcSkinChangeResult;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.api.form.definition.NpcFormChangeResult;
import io.github.kxyr0.echonpc.api.scene.NpcPlaceResult;
import java.util.Optional;

final class EchoNpcCommandFormatting {
    private EchoNpcCommandFormatting() {
    }

    static String mutation(NpcResults.Spawn result) {
        return "spawn status=" + result.status() + operationSuffix(result.operationId()) + " " + result.message();
    }

    static String mutation(NpcResults.Teleport result) {
        return "teleport status=" + result.status() + operationSuffix(result.operationId()) + " "
                + result.message();
    }

    static String mutation(NpcResults.Despawn result) {
        return "despawn status=" + result.status() + operationSuffix(result.operationId()) + " "
                + result.message();
    }

    static String mutation(NpcFormChangeResult result) {
        return "set_form status=" + result.status() + " outcome=" + result.outcome()
                + operationSuffix(result.operationId()) + " " + result.message();
    }

    static String mutation(NpcSkinChangeResult result) {
        return "change_skin status=" + result.status() + " skin=" + result.skinId()
                + operationSuffix(result.operationId()) + " " + result.message();
    }

    static String mutation(NpcPlaceResult result) {
        return "place_at status=" + result.status() + operationSuffix(result.operationId()) + " "
                + result.message();
    }

    static String operation(NpcResults.Operation operation) {
        return operation.operationId() + " kind=" + operation.kind() + " status=" + operation.status()
                + " actor=" + operation.actorId() + " " + operation.message();
    }

    private static String operationSuffix(Optional<String> operationId) {
        return operationId.map(id -> " operationId=" + id).orElse("");
    }
}
