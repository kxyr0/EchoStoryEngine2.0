package io.github.kxyr0.echonpcinteraction.core.receipt;

import io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionCommandId;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionReceipt;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class NpcInteractionReceiptLedger {
    private final Map<NpcInteractionCommandId, NpcInteractionReceipt> receipts = new LinkedHashMap<>();

    public Optional<NpcInteractionReceipt> find(NpcInteractionCommandId commandId) {
        return Optional.ofNullable(receipts.get(commandId));
    }

    public NpcInteractionReceipt save(NpcInteractionReceipt receipt) {
        NpcInteractionReceipt existing = receipts.putIfAbsent(receipt.commandId(), receipt);
        return existing == null ? receipt : existing;
    }
}
