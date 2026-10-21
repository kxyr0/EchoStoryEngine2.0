package io.github.kxyr0.echotrigger.forge.domain;

import io.github.kxyr0.echoevents.api.catalog.EventKind;
import io.github.kxyr0.echoevents.api.identity.EventIds.EventSubscriptionId;
import io.github.kxyr0.echoevents.api.identity.EventIds.ResourceId;
import io.github.kxyr0.echoevents.api.identity.EventIds.SymbolId;
import io.github.kxyr0.echoevents.api.subscription.EventFilter;
import io.github.kxyr0.echoevents.api.subscription.EventSubscription;
import io.github.kxyr0.echotrigger.api.domain.EventTriggerDefinition;
import io.github.kxyr0.echotrigger.api.domain.EventTriggerDefinition.Location;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

final class EventTriggerDefinitionCodec {
    void write(DataOutputStream output, EventTriggerDefinition definition) throws IOException {
        output.writeInt(1);
        writeSubscription(output, definition.subscription());
        writeLocation(output, definition.matcherSource());
        output.writeInt(definition.actions().size());
        for (var action : definition.actions()) {
            output.writeUTF(action.kind().name());
            output.writeUTF(action.target().value());
            writeLocation(output, action.source());
        }
    }

    EventTriggerDefinition read(DataInputStream input) throws IOException {
        if (input.readInt() != 1) {
            throw new IOException("Unsupported trigger archive schema");
        }
        var subscription = readSubscription(input);
        var source = readLocation(input);
        int count = input.readInt();
        if (count < 1 || count > 180) {
            throw new IOException("Invalid trigger action count");
        }
        var actions = new ArrayList<EventTriggerDefinition.Action>();
        for (int index = 0; index < count; index++) {
            actions.add(new EventTriggerDefinition.Action(EventTriggerDefinition.Kind.valueOf(input.readUTF()),
                    new ResourceId(input.readUTF()), readLocation(input)));
        }
        return new EventTriggerDefinition(subscription, source, actions);
    }

    private void writeSubscription(DataOutputStream output, EventSubscription subscription) throws IOException {
        output.writeUTF(subscription.id().value());
        output.writeUTF(subscription.filter().kind().stableId());
        output.writeUTF(subscription.filter().resource().value());
        writeSymbol(output, subscription.filter().detail());
        writeSymbol(output, subscription.filter().subject());
        output.writeUTF(subscription.revision());
        output.writeBoolean(subscription.once());
    }

    private EventSubscription readSubscription(DataInputStream input) throws IOException {
        var id = new EventSubscriptionId(input.readUTF());
        var kind = EventKind.fromStableId(input.readUTF());
        var resource = new ResourceId(input.readUTF());
        var detail = readSymbol(input);
        var subject = readSymbol(input);
        return new EventSubscription(id, new EventFilter(kind, resource, detail, subject),
                input.readUTF(), input.readBoolean());
    }

    private void writeSymbol(DataOutputStream output, Optional<SymbolId> symbol) throws IOException {
        output.writeBoolean(symbol.isPresent());
        if (symbol.isPresent()) {
            output.writeUTF(symbol.get().value());
        }
    }

    private Optional<SymbolId> readSymbol(DataInputStream input) throws IOException {
        return input.readBoolean() ? Optional.of(new SymbolId(input.readUTF())) : Optional.empty();
    }

    private void writeLocation(DataOutputStream output, Location source) throws IOException {
        output.writeUTF(source.sourceId());
        output.writeInt(source.line());
        output.writeInt(source.column());
    }

    private Location readLocation(DataInputStream input) throws IOException {
        return new Location(input.readUTF(), input.readInt(), input.readInt());
    }
}
