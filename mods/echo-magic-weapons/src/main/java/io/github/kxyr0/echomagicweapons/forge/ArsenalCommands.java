package io.github.kxyr0.echomagicweapons.forge;

import com.mojang.brigadier.CommandDispatcher;
import io.github.kxyr0.echomagic.api.player.MagicPlayerRef;
import io.github.kxyr0.echomagic.forge.api.MagicService;
import io.github.kxyr0.echomagic.forge.api.lookup.EchoMagicServiceLookupEvent;
import io.github.kxyr0.echomagicweapons.forge.item.ArsenalWeaponKind;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;

public final class ArsenalCommands {
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echo_magic_weapons").requires(source -> source.hasPermission(2))
                .then(Commands.literal("setup").then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> setup(context.getSource(),
                                EntityArgument.getPlayer(context, "player"))))));
    }

    private int setup(CommandSourceStack source, ServerPlayer player) {
        EchoMagicServiceLookupEvent lookup = new EchoMagicServiceLookupEvent(source.getServer());
        MinecraftForge.EVENT_BUS.post(lookup);
        if (lookup.providers().isEmpty()) return message(source, "command.echo_magic_weapons.unavailable");
        MagicService magic = lookup.providers().get(0);
        MagicPlayerRef ref = new MagicPlayerRef(player.getUUID());
        for (ArsenalWeaponKind kind : ArsenalWeaponKind.values()) {
            magic.grant(ref, ArsenalIds.spell(kind));
            magic.clearCooldown(ref, ArsenalIds.spell(kind));
        }
        magic.select(ref, ArsenalIds.FIRE_SPEAR);
        return message(source, "command.echo_magic_weapons.success", player.getDisplayName());
    }

    private int message(CommandSourceStack source, String key, Object... arguments) {
        source.sendSuccess(() -> Component.translatable(key, arguments), false);
        return 1;
    }
}
