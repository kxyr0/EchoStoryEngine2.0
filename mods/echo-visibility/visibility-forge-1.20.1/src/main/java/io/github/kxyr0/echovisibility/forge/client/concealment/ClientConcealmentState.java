package io.github.kxyr0.echovisibility.forge.client.concealment;

import io.github.kxyr0.echovisibility.api.identity.VisibilityConcealmentLeaseId;
import io.github.kxyr0.echovisibility.api.state.VisibilityConcealmentStatus;
import io.github.kxyr0.echovisibility.api.target.VisibilityNpcTarget;
import io.github.kxyr0.echovisibility.core.concealment.VisibilityConcealmentLease;
import io.github.kxyr0.echovisibility.core.concealment.VisibilityConcealmentLedger;
import io.github.kxyr0.echovisibility.forge.client.runtime.ClientArmedWatch;
import io.github.kxyr0.echovisibility.forge.network.ClientboundConcealmentPacket;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ClientConcealmentState {
    private static final long TIMEOUT_TICKS = 10L;
    private final VisibilityConcealmentLedger ledger = new VisibilityConcealmentLedger();
    private final Map<String, VisibilityConcealmentStatus> lastWatchStatus = new HashMap<>();

    public Optional<VisibilityConcealmentLeaseId> conceal(
            ClientArmedWatch watch,
            long sequence,
            long clientTick) {
        if (!(watch.definition().target() instanceof VisibilityNpcTarget npc)) {
            return Optional.empty();
        }
        VisibilityConcealmentLeaseId id = new VisibilityConcealmentLeaseId(
                watch.definition().id().value() + "/" + watch.generation().value() + "/" + sequence);
        VisibilityConcealmentLease lease = new VisibilityConcealmentLease(id, watch.definition().id(),
                watch.generation(), watch.definition().revision(), npc.npcId(), sequence,
                clientTick + TIMEOUT_TICKS);
        if (!ledger.add(lease)) {
            return Optional.empty();
        }
        lastWatchStatus.put(watch.definition().id().value(), VisibilityConcealmentStatus.PENDING);
        return Optional.of(id);
    }

    public void apply(ClientboundConcealmentPacket packet) {
        ledger.find(packet.leaseId()).ifPresent(lease -> {
            switch (packet.status()) {
                case CLAIMED -> lease.claim();
                case COMMITTED -> lease.commit();
                case REJECTED, TIMED_OUT -> lease.reject();
                default -> {
                }
            }
            lastWatchStatus.put(lease.snapshot().watchId().value(), packet.status());
        });
    }

    public void tick(long clientTick) {
        for (var expired : ledger.expire(clientTick)) {
            lastWatchStatus.put(expired.watchId().value(), VisibilityConcealmentStatus.TIMED_OUT);
        }
    }

    public boolean hidesNpc(String npcId) {
        return ledger.hidesNpc(npcId);
    }

    public VisibilityConcealmentStatus status(ClientArmedWatch watch) {
        return lastWatchStatus.getOrDefault(watch.definition().id().value(), VisibilityConcealmentStatus.NONE);
    }

    public void disarm(ClientArmedWatch watch) {
        ledger.snapshots().stream().filter(value -> value.watchId().equals(watch.definition().id()))
                .flatMap(value -> ledger.find(value.id()).stream()).forEach(VisibilityConcealmentLease::reject);
        lastWatchStatus.remove(watch.definition().id().value());
    }
}
