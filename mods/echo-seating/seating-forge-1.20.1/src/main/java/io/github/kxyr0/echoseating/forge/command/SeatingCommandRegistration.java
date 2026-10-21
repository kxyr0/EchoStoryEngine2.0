package io.github.kxyr0.echoseating.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import io.github.kxyr0.echoseating.api.id.SeatZoneId;
import io.github.kxyr0.echoseating.api.zone.SeatZoneDefinition;
import io.github.kxyr0.echoseating.forge.runtime.ForgeSeatingServices;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;

public final class SeatingCommandRegistration {
    private SeatingCommandRegistration() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, ForgeSeatingServices services) {
        dispatcher.register(Commands.literal("echo_seating")
                .then(Commands.literal("zones").executes(ctx -> zones(ctx.getSource(), services)))
                .then(Commands.literal("occupancy").executes(ctx -> occupancy(ctx.getSource(), services)))
                .then(Commands.literal("diagnostics").executes(ctx -> diagnostics(ctx.getSource(), services)))
                .then(Commands.literal("reload").executes(ctx -> reload(ctx.getSource(), services)))
                .then(Commands.literal("debug")
                        .then(Commands.literal("off").executes(ctx -> say(ctx.getSource(), "debug off")))
                        .then(Commands.literal("zone")
                                .then(Commands.argument("zone_id", ResourceLocationArgument.id())
                                        .executes(ctx -> inspectZone(ctx.getSource(), services,
                                                zoneId(ctx, "zone_id"))))))
                .then(Commands.literal("zone").then(Commands.literal("inspect")
                        .then(Commands.argument("zone_id", ResourceLocationArgument.id())
                                .executes(ctx -> inspectZone(ctx.getSource(), services,
                                        zoneId(ctx, "zone_id"))))))
                .then(Commands.literal("slot").then(Commands.literal("inspect")
                        .then(Commands.argument("zone_id", ResourceLocationArgument.id())
                                .then(Commands.argument("slot_id", ResourceLocationArgument.id())
                                        .executes(ctx -> inspectSlot(ctx.getSource(), services,
                                                zoneId(ctx, "zone_id"), zoneId(ctx, "slot_id")))))))
                .then(Commands.literal("player").then(Commands.literal("inspect")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> say(ctx.getSource(), "player inspect ready"))))));
    }

    private static int zones(CommandSourceStack source, ForgeSeatingServices services) {
        return say(source, "zones=" + services.zones().size() + ", revision=" + services.catalogRevision());
    }

    private static int occupancy(CommandSourceStack source, ForgeSeatingServices services) {
        return say(source, "occupancy=" + services.activeOccupancies().size());
    }

    private static int diagnostics(CommandSourceStack source, ForgeSeatingServices services) {
        services.diagnostics().stream().limit(8L)
                .forEach(diagnostic -> source.sendSuccess(() -> Component.literal(
                        diagnostic.code() + " " + diagnostic.subject() + " " + diagnostic.message()), false));
        return services.diagnostics().size();
    }

    private static int reload(CommandSourceStack source, ForgeSeatingServices services) {
        services.server().ifPresent(server -> services.reload(server.getWorldPath(LevelResource.ROOT)));
        return say(source, "reload complete");
    }

    private static int inspectZone(CommandSourceStack source, ForgeSeatingServices services, String zoneId) {
        return say(source, services.find(new SeatZoneId(zoneId))
                .map(SeatingCommandRegistration::formatZone)
                .orElse("zone not found: " + zoneId));
    }

    private static int inspectSlot(CommandSourceStack source, ForgeSeatingServices services,
            String zoneId, String slotId) {
        return say(source, services.find(new SeatZoneId(zoneId))
                .flatMap(zone -> zone.slots().stream()
                        .filter(slot -> slot.id().value().equals(slotId))
                        .findFirst()
                        .map(slot -> slot.id().value() + " anchor=" + slot.anchor()))
                .orElse("slot not found: " + slotId + " in " + zoneId));
    }

    private static String formatZone(SeatZoneDefinition zone) {
        return zone.id().value() + " dimension=" + zone.dimension().value()
                + " area=" + zone.area().min() + ".." + zone.area().max()
                + " slots=" + zone.slots().size();
    }

    private static String zoneId(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, String name) {
        return ResourceLocationArgument.getId(ctx, name).toString();
    }

    private static int say(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }
}
