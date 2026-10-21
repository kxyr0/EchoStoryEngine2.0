package io.github.kxyr0.echomagic.core.hit;

import io.github.kxyr0.echomagic.api.hit.MagicHitReceipt;
import io.github.kxyr0.echomagic.api.identity.SpellProjectileId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class MagicHitLedger {
    private final Map<SpellProjectileId, MagicHitReceipt> receipts = new LinkedHashMap<>();
    private final int maximumReceipts;

    public MagicHitLedger(int maximumReceipts) { this.maximumReceipts = maximumReceipts; }

    public synchronized boolean record(MagicHitReceipt receipt) {
        if (receipts.containsKey(receipt.projectileId())) return false;
        receipts.put(receipt.projectileId(), receipt);
        while (receipts.size() > maximumReceipts) receipts.remove(receipts.keySet().iterator().next());
        return true;
    }

    public synchronized Optional<MagicHitReceipt> find(SpellProjectileId projectile) {
        return Optional.ofNullable(receipts.get(projectile));
    }
}
