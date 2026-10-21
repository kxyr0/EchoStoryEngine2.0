package io.github.kxyr0.echoscene.api.definition;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echoscene.api.identity.SceneStepId;
import io.github.kxyr0.echoscene.api.model.BlockingMode;
import java.util.List;
import java.util.Objects;

public record SceneCommand(
        SceneStepId stepId,
        String receiver,
        String operation,
        List<String> arguments,
        BlockingMode blockingMode,
        List<SceneCapability> capabilities,
        SourceSpan span
) {
    public SceneCommand {
        stepId = Objects.requireNonNull(stepId, "stepId");
        receiver = Objects.requireNonNull(receiver, "receiver");
        operation = Objects.requireNonNull(operation, "operation");
        arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
        blockingMode = Objects.requireNonNull(blockingMode, "blockingMode");
        capabilities = capabilities.stream().distinct().sorted().toList();
        span = Objects.requireNonNull(span, "span");
    }
}
