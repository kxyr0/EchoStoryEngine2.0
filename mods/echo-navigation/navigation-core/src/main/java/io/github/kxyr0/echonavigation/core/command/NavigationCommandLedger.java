package io.github.kxyr0.echonavigation.core.command;

import io.github.kxyr0.echonavigation.api.identity.NavigationCommandId;
import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Durable idempotency ledger for script commands.
 */
public final class NavigationCommandLedger {
    private final Map<NavigationCommandId, NavigationMoveId> movesByCommand = new LinkedHashMap<>();

    public CommandLedgerResult bind(NavigationCommandId commandId, NavigationMoveId moveId) {
        NavigationMoveId existing = movesByCommand.get(Objects.requireNonNull(commandId, "commandId"));
        if (existing == null) {
            movesByCommand.put(commandId, Objects.requireNonNull(moveId, "moveId"));
            return new CommandLedgerResult(CommandLedgerResult.Status.RECORDED, Optional.of(moveId));
        }
        CommandLedgerResult.Status status = existing.equals(moveId)
                ? CommandLedgerResult.Status.DUPLICATE
                : CommandLedgerResult.Status.CONFLICT;
        return new CommandLedgerResult(status, Optional.of(existing));
    }

    public Optional<NavigationMoveId> existing(NavigationCommandId commandId) {
        return Optional.ofNullable(movesByCommand.get(commandId));
    }

    public Map<NavigationCommandId, NavigationMoveId> snapshot() {
        return Map.copyOf(movesByCommand);
    }

    public void restore(Map<NavigationCommandId, NavigationMoveId> entries) {
        movesByCommand.clear();
        movesByCommand.putAll(Objects.requireNonNull(entries, "entries"));
    }
}
