package io.github.kxyr0.echodevscenarios.forge.network;

import io.github.kxyr0.echodevscenarios.definition.ScenarioDefinition;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;

public record OpenScenarioScreenMessage(List<Entry> entries) {
    public OpenScenarioScreenMessage {
        entries = List.copyOf(entries);
    }

    static OpenScenarioScreenMessage from(List<ScenarioDefinition> definitions) {
        return new OpenScenarioScreenMessage(definitions.stream().map(value -> new Entry(value.id().value(),
                value.title(), value.groups(), value.target().resourceId())).toList());
    }

    static void encode(OpenScenarioScreenMessage message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.entries.size());
        message.entries.forEach(value -> {
            buffer.writeUtf(value.id);
            buffer.writeUtf(value.title);
            buffer.writeCollection(value.groups, FriendlyByteBuf::writeUtf);
            buffer.writeUtf(value.target);
        });
    }

    static OpenScenarioScreenMessage decode(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        java.util.ArrayList<Entry> entries = new java.util.ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            entries.add(new Entry(buffer.readUtf(), buffer.readUtf(), buffer.readList(FriendlyByteBuf::readUtf),
                    buffer.readUtf()));
        }
        return new OpenScenarioScreenMessage(entries);
    }

    public record Entry(String id, String title, List<String> groups, String target) {
        public Entry { groups = List.copyOf(groups); }
    }
}
