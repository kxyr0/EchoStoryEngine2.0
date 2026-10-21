package io.github.kxyr0.echobackgroundnpc.echoscript.binding;

import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.AmbientDialogueId;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundLocationId;
import io.github.kxyr0.echobackgroundnpc.api.task.BackgroundTask;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.Map;
import java.util.Optional;

final class BackgroundTaskReader {
    private final BackgroundCallReader calls = new BackgroundCallReader();

    Optional<BackgroundTask> read(InvocationExpressionSyntax call, Map<String, BackgroundNpcId> symbols) {
        return switch (calls.name(call)) {
            case "GoTo" -> calls.string(call, 0).map(BackgroundLocationId::new).map(BackgroundTask.Travel::new);
            case "StayAt" -> calls.string(call, 0).map(BackgroundLocationId::new).map(BackgroundTask.Stay::new);
            case "Perform" -> calls.string(call, 0).map(animation -> new BackgroundTask.Perform(animation,
                    calls.namedString(call, "at").map(BackgroundLocationId::new)));
            case "TalkWith" -> conversation(call, symbols);
            default -> Optional.empty();
        };
    }

    private Optional<BackgroundTask> conversation(InvocationExpressionSyntax call,
            Map<String, BackgroundNpcId> symbols) {
        Optional<BackgroundNpcId> participant = calls.symbol(call, 0).map(symbols::get);
        Optional<AmbientDialogueId> dialogue = calls.string(call, 1).map(AmbientDialogueId::new);
        if (participant.isEmpty() || dialogue.isEmpty()) return Optional.empty();
        return Optional.of(new BackgroundTask.Converse(participant.orElseThrow(), dialogue.orElseThrow(),
                calls.namedString(call, "at").map(BackgroundLocationId::new)));
    }
}
