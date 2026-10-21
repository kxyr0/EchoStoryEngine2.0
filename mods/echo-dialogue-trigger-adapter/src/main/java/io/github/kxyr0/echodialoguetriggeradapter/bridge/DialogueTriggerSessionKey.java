package io.github.kxyr0.echodialoguetriggeradapter.bridge;

import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import java.util.Objects;

public record DialogueTriggerSessionKey(
        DialogueSessionId sessionId,
        String nodeId,
        int commandPosition,
        String expressionFingerprint
) {
    public DialogueTriggerSessionKey {
        sessionId = Objects.requireNonNull(sessionId, "sessionId");
        nodeId = Objects.requireNonNull(nodeId, "nodeId").trim();
        expressionFingerprint = Objects.requireNonNull(expressionFingerprint, "expressionFingerprint").trim();
        if (nodeId.isEmpty() || expressionFingerprint.isEmpty() || commandPosition < 0) {
            throw new IllegalArgumentException("Dialogue trigger key fields must be stable and non-empty.");
        }
    }

    public String stableSource() {
        return sessionId.value() + "|" + nodeId + "|" + commandPosition + "|" + expressionFingerprint;
    }
}
