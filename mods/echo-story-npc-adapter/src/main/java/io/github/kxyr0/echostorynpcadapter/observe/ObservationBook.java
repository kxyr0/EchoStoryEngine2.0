package io.github.kxyr0.echostorynpcadapter.observe;

import io.github.kxyr0.echonpc.api.service.NpcService;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionAcceptance;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Bounded server-scoped runtime observations for pending Story actions.
 */
public final class ObservationBook {
    private static final int CAPACITY = 128;
    private final Map<String, ActionObservation> observations = new LinkedHashMap<>();
    private final NpcPostconditions postconditions = new NpcPostconditions();

    public RegisterStatus register(ActionObservation observation) {
        ActionObservation checked = Objects.requireNonNull(observation, "observation");
        ActionObservation existing = observations.get(checked.key());
        if (existing != null) {
            return sameRequest(existing, checked) ? RegisterStatus.DUPLICATE : RegisterStatus.CONFLICT;
        }
        if (observations.size() >= CAPACITY) {
            return RegisterStatus.FULL;
        }
        observations.put(checked.key(), checked);
        return RegisterStatus.ADDED;
    }

    public void tick(NpcService service) {
        for (ActionObservation observation : new ArrayList<>(observations.values())) {
            postconditions.completion(observation, service).ifPresent(completion -> {
                ActionCompletionAcceptance accepted = observation.completionSink().complete(completion);
                if (accepted == ActionCompletionAcceptance.ACCEPTED
                        || accepted == ActionCompletionAcceptance.DUPLICATE
                        || accepted == ActionCompletionAcceptance.UNKNOWN_ACTION) {
                    observations.remove(observation.key());
                }
            });
        }
    }

    public int size() {
        return observations.size();
    }

    public void close() {
        observations.clear();
    }

    private boolean sameRequest(ActionObservation left, ActionObservation right) {
        return left.actionType().equals(right.actionType())
                && left.actorId().equals(right.actorId())
                && left.payloadFingerprint().equals(right.payloadFingerprint());
    }

    public enum RegisterStatus {
        ADDED,
        DUPLICATE,
        CONFLICT,
        FULL
    }
}
