package io.github.kxyr0.echoscene.compile;

import io.github.kxyr0.echoscene.api.definition.SceneCapability;
import io.github.kxyr0.echoscene.api.model.BlockingMode;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public record SceneCommandRule(
        SceneSymbolKind receiverKind,
        String receiverName,
        String method,
        List<SceneArgumentKind> arguments,
        BlockingMode blockingMode,
        Function<SceneSymbol, List<SceneCapability>> capabilities
) {
    public SceneCommandRule {
        receiverKind = Objects.requireNonNull(receiverKind, "receiverKind");
        receiverName = Objects.requireNonNull(receiverName, "receiverName");
        method = Objects.requireNonNull(method, "method");
        arguments = List.copyOf(arguments);
        blockingMode = Objects.requireNonNull(blockingMode, "blockingMode");
        capabilities = Objects.requireNonNull(capabilities, "capabilities");
    }
}
