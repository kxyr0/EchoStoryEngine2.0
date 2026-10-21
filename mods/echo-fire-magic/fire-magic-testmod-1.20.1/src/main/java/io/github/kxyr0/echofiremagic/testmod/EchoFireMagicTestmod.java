package io.github.kxyr0.echofiremagic.testmod;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(EchoFireMagicTestmod.MOD_ID)
public final class EchoFireMagicTestmod {
    public static final String MOD_ID = "echo_fire_magic_test";

    public EchoFireMagicTestmod() {
        MinecraftForge.EVENT_BUS.addListener(this::commands);
    }

    private void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("echo_fire_magic_test")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("acceptance").executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal(
                            "Run /echo_magic_test arena, /echo_fire_magic setup <player>, "
                                    + "then use the configured cast control."), false);
                    return 1;
                })));
    }
}

