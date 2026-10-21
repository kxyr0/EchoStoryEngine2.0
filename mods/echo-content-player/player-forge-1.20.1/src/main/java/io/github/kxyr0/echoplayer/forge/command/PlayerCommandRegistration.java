package io.github.kxyr0.echoplayer.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.kxyr0.echoplayer.api.identity.EchoPlayerId;
import io.github.kxyr0.echoplayer.forge.api.query.PlayerProfileQueryService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Minimal production command surface for profile inspection and diagnostics.
 */
public final class PlayerCommandRegistration {
    private final PlayerProfileQueryService profiles;

    public PlayerCommandRegistration(PlayerProfileQueryService profiles) {
        this.profiles = profiles;
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echoplayer")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("inspect").executes(context -> inspect(context.getSource()))));
    }

    private int inspect(CommandSourceStack source) throws CommandSyntaxException {
        EchoPlayerId playerId = EchoPlayerId.fromAccountUuid(source.getPlayerOrException().getUUID());
        profiles.find(playerId).ifPresentOrElse(snapshot -> source.sendSuccess(() -> Component.literal(
                "EchoPlayerId=" + snapshot.playerId().value()
                        + " name=" + snapshot.identity().resolvedDisplayName()
                        + " spark=" + snapshot.spark().current() + "/" + snapshot.spark().maximum()
                        + " revision=" + snapshot.profileRevision().value()), false),
                () -> source.sendFailure(Component.literal("Echo profile is not loaded.")));
        return 1;
    }
}
