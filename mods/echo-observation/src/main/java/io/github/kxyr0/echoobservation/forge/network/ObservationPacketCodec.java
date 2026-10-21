package io.github.kxyr0.echoobservation.forge.network;

import io.github.kxyr0.echoobservation.api.action.ObservationScriptAction;
import io.github.kxyr0.echoobservation.api.action.ObservationScriptArgument;
import io.github.kxyr0.echoobservation.api.action.ObservationScriptBlock;
import io.github.kxyr0.echoobservation.api.identity.ObservationId;
import io.github.kxyr0.echoobservation.api.identity.ObservationResourceId;
import io.github.kxyr0.echoobservation.api.model.ObservationAnchor;
import io.github.kxyr0.echoobservation.api.model.ObservationDefinition;
import io.github.kxyr0.echoobservation.api.model.ObservationDuration;
import io.github.kxyr0.echoobservation.api.model.ObservationFace;
import io.github.kxyr0.echoobservation.api.model.ObservationResourceDefinition;
import io.github.kxyr0.echoobservation.api.model.ObservationSource;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;

final class ObservationPacketCodec {
    private ObservationPacketCodec() {
    }

    static void writeSnapshot(ObservationPackets.ClientboundSnapshot packet, FriendlyByteBuf buf) {
        buf.writeVarLong(packet.sequence());
        buf.writeVarLong(packet.catalogRevision());
        buf.writeVarInt(packet.resources().size());
        packet.resources().forEach(value -> writeResource(buf, value));
        writeStrings(buf, packet.activeResources());
        writeStrings(buf, packet.disabledObservations());
        writeStrings(buf, packet.completedObservations());
    }

    static ObservationPackets.ClientboundSnapshot readSnapshot(FriendlyByteBuf buf) {
        long sequence = buf.readVarLong();
        long revision = buf.readVarLong();
        int count = bounded(buf.readVarInt(), 4096);
        List<ObservationResourceDefinition> resources = new ArrayList<>();
        for (int index = 0; index < count; index++) resources.add(readResource(buf));
        return new ObservationPackets.ClientboundSnapshot(sequence, revision, resources,
                readStrings(buf), readStrings(buf), readStrings(buf));
    }

    private static void writeResource(FriendlyByteBuf buf, ObservationResourceDefinition resource) {
        buf.writeUtf(resource.id().toString());
        buf.writeVarLong(resource.revision());
        buf.writeUtf(resource.sourceId());
        buf.writeVarInt(resource.observations().size());
        resource.observations().forEach(value -> writeDefinition(buf, value));
    }

    private static ObservationResourceDefinition readResource(FriendlyByteBuf buf) {
        ObservationResourceId id = ObservationResourceId.parse(buf.readUtf(256));
        long revision = buf.readVarLong();
        String sourceId = buf.readUtf(1024);
        int count = bounded(buf.readVarInt(), 4096);
        List<ObservationDefinition> definitions = new ArrayList<>();
        for (int index = 0; index < count; index++) definitions.add(readDefinition(buf));
        return new ObservationResourceDefinition(id, definitions, revision, sourceId);
    }

    private static void writeDefinition(FriendlyByteBuf buf, ObservationDefinition value) {
        buf.writeUtf(value.id().toString());
        buf.writeUtf(value.symbol());
        buf.writeBoolean(value.anchor().isPresent());
        value.anchor().ifPresent(anchor -> writeAnchor(buf, anchor));
        buf.writeVarInt(value.focus().ticks());
        buf.writeDouble(value.distance());
        buf.writeDouble(value.screenRadius());
        buf.writeBoolean(value.repeatable());
        writeOptionalString(buf, value.text());
        writeOptionalString(buf, value.thought());
        writeActions(buf, value.onComplete());
        buf.writeCollection(value.formerLocalIds(), FriendlyByteBuf::writeUtf);
        buf.writeVarLong(value.revision());
        writeSource(buf, value.source());
    }

    private static ObservationDefinition readDefinition(FriendlyByteBuf buf) {
        ObservationId id = ObservationId.parse(buf.readUtf(512));
        String symbol = buf.readUtf(256);
        Optional<ObservationAnchor> anchor = buf.readBoolean() ? Optional.of(readAnchor(buf)) : Optional.empty();
        ObservationDuration focus = new ObservationDuration(buf.readVarInt());
        double distance = buf.readDouble();
        double radius = buf.readDouble();
        boolean repeatable = buf.readBoolean();
        Optional<String> text = readOptionalString(buf);
        Optional<String> thought = readOptionalString(buf);
        ObservationScriptBlock actions = readActions(buf);
        List<String> former = buf.readList(value -> value.readUtf(256));
        long revision = buf.readVarLong();
        return new ObservationDefinition(id, symbol, anchor, focus, distance, radius, repeatable, text, thought,
                actions, former, revision, readSource(buf));
    }

    private static void writeAnchor(FriendlyByteBuf buf, ObservationAnchor value) {
        buf.writeUtf(value.dimension());
        buf.writeInt(value.blockX()); buf.writeInt(value.blockY()); buf.writeInt(value.blockZ());
        buf.writeDouble(value.offsetX()); buf.writeDouble(value.offsetY()); buf.writeDouble(value.offsetZ());
        buf.writeEnum(value.face());
    }

    private static ObservationAnchor readAnchor(FriendlyByteBuf buf) {
        return new ObservationAnchor(buf.readUtf(256), buf.readInt(), buf.readInt(), buf.readInt(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readEnum(ObservationFace.class));
    }

    private static void writeActions(FriendlyByteBuf buf, ObservationScriptBlock block) {
        buf.writeVarInt(block.actions().size());
        block.actions().forEach(action -> {
            buf.writeUtf(action.target());
            buf.writeVarInt(action.arguments().size());
            action.arguments().forEach(argument -> {
                writeOptionalString(buf, argument.name());
                buf.writeUtf(argument.value(), 4096);
            });
            buf.writeVarInt(action.sourceOffset());
        });
    }

    private static ObservationScriptBlock readActions(FriendlyByteBuf buf) {
        int count = bounded(buf.readVarInt(), 256);
        List<ObservationScriptAction> actions = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            String target = buf.readUtf(256);
            int argumentCount = bounded(buf.readVarInt(), 64);
            List<ObservationScriptArgument> arguments = new ArrayList<>();
            for (int argument = 0; argument < argumentCount; argument++) {
                arguments.add(new ObservationScriptArgument(readOptionalString(buf), buf.readUtf(4096)));
            }
            actions.add(new ObservationScriptAction(target, arguments, buf.readVarInt()));
        }
        return new ObservationScriptBlock(actions);
    }

    private static void writeSource(FriendlyByteBuf buf, ObservationSource source) {
        buf.writeUtf(source.sourceId(), 1024);
        buf.writeVarInt(source.declarationStart()); buf.writeVarInt(source.declarationEnd());
        buf.writeVarInt(source.bodyStart()); buf.writeBoolean(source.atSpan().isPresent());
        source.atSpan().ifPresent(span -> { buf.writeVarInt(span.start()); buf.writeVarInt(span.end()); });
        buf.writeVarInt(source.line());
    }

    private static ObservationSource readSource(FriendlyByteBuf buf) {
        String sourceId = buf.readUtf(1024);
        int start = buf.readVarInt(); int end = buf.readVarInt(); int body = buf.readVarInt();
        Optional<ObservationSource.Span> at = buf.readBoolean()
                ? Optional.of(new ObservationSource.Span(buf.readVarInt(), buf.readVarInt())) : Optional.empty();
        return new ObservationSource(sourceId, start, end, body, at, buf.readVarInt());
    }

    private static void writeStrings(FriendlyByteBuf buf, Set<String> values) {
        buf.writeVarInt(values.size());
        values.stream().sorted().forEach(value -> buf.writeUtf(value, 512));
    }

    private static Set<String> readStrings(FriendlyByteBuf buf) {
        int count = bounded(buf.readVarInt(), 16384);
        Set<String> result = new LinkedHashSet<>();
        for (int index = 0; index < count; index++) result.add(buf.readUtf(512));
        return result;
    }

    private static void writeOptionalString(FriendlyByteBuf buf, Optional<String> value) {
        buf.writeBoolean(value.isPresent());
        value.ifPresent(text -> buf.writeUtf(text, 8192));
    }

    private static Optional<String> readOptionalString(FriendlyByteBuf buf) {
        return buf.readBoolean() ? Optional.of(buf.readUtf(8192)) : Optional.empty();
    }

    private static int bounded(int value, int maximum) {
        if (value < 0 || value > maximum) throw new IllegalArgumentException("Packet collection is out of bounds.");
        return value;
    }
}
