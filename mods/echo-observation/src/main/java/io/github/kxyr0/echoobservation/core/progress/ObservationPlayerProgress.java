package io.github.kxyr0.echoobservation.core.progress;

import io.github.kxyr0.echoobservation.api.action.ObservationActionExecutionId;
import io.github.kxyr0.echoobservation.api.identity.ObservationCompletionId;
import io.github.kxyr0.echoobservation.api.identity.ObservationId;
import io.github.kxyr0.echoobservation.api.identity.ObservationResourceId;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ObservationPlayerProgress {
    private final Set<ObservationResourceId> active = new LinkedHashSet<>();
    private final Set<ObservationId> disabled = new LinkedHashSet<>();
    private final Map<ObservationId, ObservationCompletionId> completed = new LinkedHashMap<>();
    private final Set<ObservationActionExecutionId> receipts = new LinkedHashSet<>();

    public boolean activate(ObservationResourceId id) { return active.add(id); }
    public boolean deactivate(ObservationResourceId id) { return active.remove(id); }
    public boolean enable(ObservationId id) { return disabled.remove(id); }
    public boolean disable(ObservationId id) { return disabled.add(id); }
    public boolean active(ObservationResourceId id) { return active.contains(id); }
    public boolean enabled(ObservationId id) { return !disabled.contains(id); }
    public boolean completed(ObservationId id) { return completed.containsKey(id); }
    public Optional<ObservationCompletionId> completion(ObservationId id) {
        return Optional.ofNullable(completed.get(id));
    }
    public boolean complete(ObservationId id, ObservationCompletionId completionId) {
        return completed.putIfAbsent(id, completionId) == null;
    }
    public boolean resetCompletion(ObservationId id) {
        ObservationCompletionId completion = completed.remove(id);
        if (completion == null) return false;
        String prefix = completion.value() + "/action/";
        receipts.removeIf(value -> value.value().startsWith(prefix));
        return true;
    }
    public boolean receipt(ObservationActionExecutionId id) { return receipts.contains(id); }
    public boolean acknowledge(ObservationActionExecutionId id) { return receipts.add(id); }
    public Set<ObservationResourceId> activeResources() { return Set.copyOf(active); }
    public Set<ObservationId> disabledObservations() { return Set.copyOf(disabled); }
    public Map<ObservationId, ObservationCompletionId> completions() { return Map.copyOf(completed); }
    public Set<ObservationActionExecutionId> receipts() { return Set.copyOf(receipts); }
    public boolean migrate(ObservationId former, ObservationId current) {
        boolean changed = disabled.remove(former);
        if (changed) disabled.add(current);
        ObservationCompletionId completion = completed.remove(former);
        if (completion != null) {
            completed.putIfAbsent(current, completion);
            changed = true;
        }
        return changed;
    }
    public void restoreActive(ObservationResourceId id) { active.add(id); }
    public void restoreDisabled(ObservationId id) { disabled.add(id); }
    public void restoreCompletion(ObservationId id, ObservationCompletionId completionId) {
        completed.put(id, completionId);
    }
    public void restoreReceipt(ObservationActionExecutionId id) { receipts.add(id); }
}
