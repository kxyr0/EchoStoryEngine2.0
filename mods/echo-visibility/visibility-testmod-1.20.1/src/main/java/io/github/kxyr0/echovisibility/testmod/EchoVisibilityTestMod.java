package io.github.kxyr0.echovisibility.testmod;

import io.github.kxyr0.echovisibility.forge.api.runtime.VisibilityServiceLookupEvent;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(EchoVisibilityTestMod.MOD_ID)
public final class EchoVisibilityTestMod {
    public static final String MOD_ID = "echo_visibility_testmod";

    public EchoVisibilityTestMod() {
        MinecraftForge.EVENT_BUS.addListener(this::commands);
    }

    private void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(net.minecraft.commands.Commands.literal("echo_visibility_test")
                .requires(source -> source.hasPermission(2))
                .executes(context -> inspect(context.getSource())));
    }

    private static int inspect(net.minecraft.commands.CommandSourceStack source) {
        VisibilityServiceLookupEvent lookup = new VisibilityServiceLookupEvent(source.getServer());
        MinecraftForge.EVENT_BUS.post(lookup);
        String status = lookup.bridge().map(value -> "resolved catalog=" + value.catalog().all().size())
                .orElse("unavailable providers=" + lookup.providerCount());
        source.sendSuccess(() -> Component.literal("VIS1 test bridge " + status), false);
        return lookup.bridge().isPresent() ? 1 : 0;
    }
}
