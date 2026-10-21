package io.github.kxyr0.echowardrobe.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import io.github.kxyr0.echowardrobe.forge.ForgeWardrobeRuntimeState;
import io.github.kxyr0.echowardrobe.forge.content.ForgeWardrobeCatalogLoader;
import java.util.Objects;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;

/**
 * Registers bounded runtime diagnostics commands.
 */
public final class WardrobeCommandRegistration {
    private final WardrobeCommandActions actions;

    public WardrobeCommandRegistration(ForgeWardrobeRuntimeState state, ForgeWardrobeCatalogLoader loader) {
        this.actions = new WardrobeCommandActions(Objects.requireNonNull(state, "state"),
                Objects.requireNonNull(loader, "loader"));
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echo_wardrobe")
                .then(Commands.literal("status").executes(actions::status))
                .then(Commands.literal("inspect")
                        .then(Commands.argument("player", EntityArgument.player()).executes(actions::inspect)))
                .then(Commands.literal("list_wearables").executes(actions::listWearables))
                .then(Commands.literal("list_outfits").executes(actions::listOutfits)
                        .then(Commands.argument("profile", ResourceLocationArgument.id())
                                .executes(actions::listOutfits)))
                .then(Commands.literal("inspect_wearable")
                        .then(Commands.argument("wearable", ResourceLocationArgument.id())
                                .executes(actions::inspectWearable)))
                .then(Commands.literal("inspect_outfit")
                        .then(Commands.argument("outfit", ResourceLocationArgument.id())
                                .executes(actions::inspectOutfit)))
                .then(Commands.literal("coverage")
                        .then(Commands.argument("profile", ResourceLocationArgument.id())
                                .executes(actions::coverage)))
                .then(Commands.literal("missing")
                        .then(Commands.argument("player", EntityArgument.player()).executes(actions::missing)))
                .then(Commands.literal("reload").executes(actions::reload))
                .then(Commands.literal("validate").executes(actions::validate))
                .then(Commands.literal("diagnostics").executes(actions::diagnostics)));
    }
}
