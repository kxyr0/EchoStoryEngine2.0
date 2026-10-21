package io.github.kxyr0.echothoughtspace.integration.observation;

import io.github.kxyr0.echoobservation.api.client.ObservationClientLookupEvent;
import io.github.kxyr0.echoobservation.api.identity.ObservationId;
import io.github.kxyr0.echothoughtspace.api.ThoughtNodeId;
import io.github.kxyr0.echothoughtspace.api.ThoughtNodeLabel;
import io.github.kxyr0.echothoughtspace.api.ThoughtNodeProvider;
import io.github.kxyr0.echothoughtspace.api.ThoughtNodeRevision;
import io.github.kxyr0.echothoughtspace.api.ThoughtNodeSnapshot;
import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.common.MinecraftForge;

public final class ObservationThoughtNodeProvider implements ThoughtNodeProvider {
    private long inputFingerprint = Long.MIN_VALUE;
    private long revision;
    private List<ThoughtNodeSnapshot> nodes = List.of();

    @Override public String providerId() { return "echo_observation"; }

    @Override
    public long revision() {
        refresh();
        return revision;
    }

    @Override public List<ThoughtNodeSnapshot> nodes() { return nodes; }

    private void refresh() {
        ObservationClientLookupEvent event = new ObservationClientLookupEvent();
        MinecraftForge.EVENT_BUS.post(event);
        var view = event.view();
        if (view.isEmpty()) {
            if (inputFingerprint != 0L) { inputFingerprint = 0L; revision++; nodes = List.of(); }
            return;
        }
        var completed = view.orElseThrow().completedObservationIds().stream().sorted().toList();
        long fingerprint = view.orElseThrow().catalog().revision();
        for (String id : completed) fingerprint = 31L * fingerprint + id.hashCode();
        if (fingerprint == inputFingerprint) return;
        List<ThoughtNodeSnapshot> next = new ArrayList<>();
        for (String value : completed) {
            try {
                view.orElseThrow().catalog().observation(ObservationId.parse(value))
                        .filter(definition -> definition.thought().filter(text -> !text.isBlank()).isPresent())
                        .ifPresent(definition -> next.add(new ThoughtNodeSnapshot(
                                new ThoughtNodeId("echo_observation:" + definition.id()),
                                new ThoughtNodeLabel(definition.thought().orElseThrow()),
                                new ThoughtNodeRevision(definition.revision()))));
            } catch (IllegalArgumentException ignored) { }
        }
        nodes = List.copyOf(next);
        inputFingerprint = fingerprint;
        revision++;
    }
}
