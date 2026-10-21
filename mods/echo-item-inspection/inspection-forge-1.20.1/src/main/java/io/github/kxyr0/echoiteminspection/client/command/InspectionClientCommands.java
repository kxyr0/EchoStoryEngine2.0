package io.github.kxyr0.echoiteminspection.client.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.kxyr0.echoiteminspection.client.api.ItemInspectionClient;
import io.github.kxyr0.echoiteminspection.forge.InspectionRuntime;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.client.Minecraft;
import io.github.kxyr0.echoiteminspection.client.screen.ItemInspectionScreen;
import io.github.kxyr0.echoiteminspection.client.world.WorldInspectionStatus;

public final class InspectionClientCommands {
    private InspectionClientCommands() { }

    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("echo_item_inspection")
                .then(InspectionPromptCommands.tree())
                .then(Commands.literal("reload").executes(context -> {
                    InspectionRuntime.reload();
                    context.getSource().sendSuccess(() -> Component.literal(InspectionRuntime.status()), false);
                    return 1;
                }))
                .then(Commands.literal("status").executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal(status()), false);
                    return 1;
                }))
                .then(Commands.literal("list").executes(context -> {
                    var definitions = InspectionRuntime.catalog().snapshot().definitions();
                    if (definitions.isEmpty()) context.getSource().sendSuccess(() -> Component.translatable(
                            "echo_item_inspection.commands.empty"), false);
                    for (var definition : definitions) {
                        context.getSource().sendSuccess(() -> Component.literal(definition.id().value() + " -> "
                                + definition.itemId() + " revision=" + definition.revision().value()), false);
                    }
                    return 1;
                }))
                .then(Commands.literal("inspect").then(Commands.argument("item-id", StringArgumentType.word())
                        .executes(context -> {
                            ResourceLocation id = ResourceLocation.tryParse(StringArgumentType.getString(context,
                                    "item-id"));
                            if (id == null || !ForgeRegistries.ITEMS.containsKey(id)) return 0;
                            var stack = ForgeRegistries.ITEMS.getValue(id).getDefaultInstance();
                            if (!ItemInspectionClient.isInspectable(stack)) {
                                context.getSource().sendFailure(Component.literal("No inspection for " + id));
                                return 0;
                            }
                            Minecraft.getInstance().tell(() -> ItemInspectionClient.openInspection(stack));
                            return 1;
                        }))));
    }

    private static String status() {
        if (Minecraft.getInstance().screen instanceof ItemInspectionScreen screen) {
            var session = screen.session();
            var parent = session.returnContext().parent();
            return "Session=" + session.id() + " state=" + session.state() + " resource="
                    + session.definition().id().value() + " item=" + session.snapshot().itemId()
                    + " fingerprint=" + session.snapshot().fingerprint() + " revision="
                    + session.definition().revision().value() + " rotation=" + session.transform().orientation()
                    + " zoom=" + session.transform().zoom() + " parent="
                    + (parent == null ? "gameplay" : parent.getClass().getName()) + " source=" + session.sourceKind()
                    + " world=" + session.worldTarget().map(target -> target.identity().toString()).orElse("none");
        }
        return InspectionRuntime.status() + "\n" + WorldInspectionStatus.describe();
    }
}
