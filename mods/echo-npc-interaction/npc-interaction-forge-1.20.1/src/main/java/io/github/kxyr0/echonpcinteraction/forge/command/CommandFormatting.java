package io.github.kxyr0.echonpcinteraction.forge.command;

import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionResult;
import io.github.kxyr0.echonpcinteraction.forge.api.provider.NpcInteractionProvider;
import java.util.stream.Collectors;

final class CommandFormatting {
    private CommandFormatting() {
    }

    static String result(NpcInteractionResult result) {
        String failure = result.failure().map(Enum::name).orElse("none");
        String receipt = result.receipt().map(CommandFormatting::receipt).orElse("noReceipt");
        String outcome = result.status().terminal() ? result.outcome().name() : "pending";
        return "status=" + result.status() + " outcome=" + outcome + " failure=" + failure
                + " commandId=" + result.commandId().value() + " " + receipt + " message=" + result.message();
    }

    private static String receipt(io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionReceipt value) {
        return "receiptRevision=" + value.receiptRevision()
                + " consequences=" + value.consequenceKinds()
                + " ride=" + value.rideHandoffStrategy().map(Enum::name).orElse("none")
                + " anchor=" + value.echoSeatAnchorId().orElse("none")
                + " occupancy=" + value.seatOccupancyId().orElse("none");
    }

    static String provider(NpcInteractionProvider provider) {
        return provider.id().value();
    }

    static String providers(java.util.List<NpcInteractionProvider> providers) {
        return providers.stream().map(CommandFormatting::provider).collect(Collectors.joining("\n"));
    }
}
