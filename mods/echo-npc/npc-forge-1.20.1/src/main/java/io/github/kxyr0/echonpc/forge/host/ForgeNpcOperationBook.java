package io.github.kxyr0.echonpc.forge.host;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.service.NpcActorView;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Bounded server-memory operation read model.
 */
public final class ForgeNpcOperationBook {
    private static final int CAPACITY = 256;
    private final AtomicLong sequence = new AtomicLong();
    private final Map<String, NpcResults.Operation> operations = new LinkedHashMap<>();
    private final ArrayDeque<String> order = new ArrayDeque<>();

    public String nextId() {
        return "op-" + sequence.incrementAndGet();
    }

    public void queued(String id, NpcResults.OperationKind kind, ActorId actorId, String message) {
        put(new NpcResults.Operation(id, kind, NpcResults.OperationStatus.QUEUED, actorId,
                Optional.empty(), message));
    }

    public void running(String id) {
        update(id, NpcResults.OperationStatus.RUNNING, Optional.empty(), "Операция выполняется.");
    }

    public void succeeded(String id, Optional<NpcActorView> actor, String message) {
        update(id, NpcResults.OperationStatus.SUCCEEDED, actor, message);
    }

    public void failed(String id, Optional<NpcActorView> actor, String message) {
        update(id, NpcResults.OperationStatus.FAILED, actor, message);
    }

    public void rejected(String id, NpcResults.OperationKind kind, ActorId actorId, String message) {
        put(new NpcResults.Operation(id, kind, NpcResults.OperationStatus.REJECTED, actorId,
                Optional.empty(), message));
    }

    public NpcResults.QueryOperation query(String id) {
        NpcResults.Operation operation = operations.get(id);
        if (operation == null) {
            return new NpcResults.QueryOperation(NpcResults.OperationQueryStatus.NOT_FOUND,
                    Optional.empty(), "Операция не найдена.");
        }
        return new NpcResults.QueryOperation(NpcResults.OperationQueryStatus.FOUND,
                Optional.of(operation), "Операция найдена.");
    }

    public NpcResults.ListOperations list(int limit) {
        int bounded = Math.max(0, Math.min(limit, CAPACITY));
        List<NpcResults.Operation> items = order.stream()
                .map(operations::get)
                .limit(bounded)
                .toList();
        return new NpcResults.ListOperations(items, operations.size() > bounded);
    }

    public int queuedCount() {
        return (int) operations.values().stream()
                .filter(item -> item.status() == NpcResults.OperationStatus.QUEUED
                        || item.status() == NpcResults.OperationStatus.RUNNING)
                .count();
    }

    private void update(String id, NpcResults.OperationStatus status, Optional<NpcActorView> actor, String message) {
        NpcResults.Operation current = operations.get(id);
        if (current != null) {
            put(new NpcResults.Operation(id, current.kind(), status, current.actorId(), actor, message));
        }
    }

    private void put(NpcResults.Operation operation) {
        if (!operations.containsKey(operation.operationId())) {
            order.addFirst(operation.operationId());
        }
        operations.put(operation.operationId(), operation);
        while (order.size() > CAPACITY) {
            operations.remove(order.removeLast());
        }
    }
}
