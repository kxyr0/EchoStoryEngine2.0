package io.github.kxyr0.echoteleportmagic.runtime;

import io.github.kxyr0.echoteleportmagic.api.TeleportAnchor;
import io.github.kxyr0.echoteleportmagic.api.TeleportExecution;
import io.github.kxyr0.echoteleportmagic.api.TeleportExecutionId;

/** Server thread boundary. prepare must reject before any visible side effect. */
public interface TeleportPort {
    void prepare(TeleportExecution execution);
    boolean alive(TeleportExecution execution);
    TeleportAnchor anchor(TeleportExecution execution);
    void visualStart(TeleportExecution execution);
    boolean commit(TeleportExecution execution);
    void visualCommit(TeleportExecution execution);
    void finished(TeleportExecution execution);
    void receipt(TeleportExecution execution, ReceiptStage stage);
    ReceiptStage receipt(TeleportExecutionId id);

    enum ReceiptStage { NONE, BEFORE_COMMIT, COMMIT_INTENT, AFTER_COMMIT, COMPLETED, CANCELLED, FAILED }
}
