package io.github.kxyr0.echofiremagic.forge;

import com.mojang.brigadier.CommandDispatcher;
import io.github.kxyr0.echomagic.api.player.MagicPlayerRef;
import io.github.kxyr0.echomagic.forge.api.MagicService;
import io.github.kxyr0.echomagic.forge.api.lookup.EchoMagicServiceLookupEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;

public final class FireMagicCommands {
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echo_fire_magic")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("setup")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> setup(context.getSource(),
                                        EntityArgument.getPlayer(context, "player"))))));
    }

    private int setup(CommandSourceStack source, ServerPlayer player) {
        EchoMagicServiceLookupEvent lookup = new EchoMagicServiceLookupEvent(source.getServer());
        MinecraftForge.EVENT_BUS.post(lookup);
        if (lookup.providers().isEmpty()) {
            return message(source, Component.translatable(
                    "command.echo_fire_magic.setup.unavailable"));
        }
        MagicService magic = lookup.providers().get(0);
        MagicPlayerRef ref = new MagicPlayerRef(player.getUUID());
        magic.grant(ref, FireMagicIds.FIRE_BOLT);
        magic.grant(ref, FireMagicIds.FIRE_SWEEP);
        magic.grant(ref, FireMagicIds.FIRE_RING);
        magic.grant(ref, FireMagicIds.BLACKSMITH_HAND);
        if (!magic.select(ref, FireMagicIds.FIRE_BOLT)) {
            return message(source, Component.translatable(
                    "command.echo_fire_magic.setup.not_loaded"));
        }
        magic.clearCooldown(ref, FireMagicIds.FIRE_BOLT);
        magic.clearCooldown(ref, FireMagicIds.FIRE_SWEEP);
        magic.clearCooldown(ref, FireMagicIds.FIRE_RING);
        magic.clearCooldown(ref, FireMagicIds.BLACKSMITH_HAND);
        return message(source, Component.translatable(
                "command.echo_fire_magic.setup.success", player.getDisplayName(),
                Component.translatable("key.echo_magic.cast_selected")));
    }

    private int message(CommandSourceStack source, Component value) {
        source.sendSuccess(() -> value, false);
        return 1;
    }
}
