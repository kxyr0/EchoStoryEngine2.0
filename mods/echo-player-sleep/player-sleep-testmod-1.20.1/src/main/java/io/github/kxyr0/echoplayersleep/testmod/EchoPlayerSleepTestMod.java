package io.github.kxyr0.echoplayersleep.testmod;

import com.mojang.brigadier.CommandDispatcher;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepCommandId;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepOwnerId;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepScenarioId;
import io.github.kxyr0.echoplayersleep.api.world.PlayerRef;
import io.github.kxyr0.echoplayersleep.forge.api.PlayerSleepServiceLookupEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(EchoPlayerSleepTestMod.MOD_ID)
public final class EchoPlayerSleepTestMod {
    public static final String MOD_ID = "echo_player_sleep_testmod";

    public EchoPlayerSleepTestMod() {
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("playersleeptest")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("armTestDream")
                        .executes(context -> armTestDream(context.getSource()))));
    }

    private int armTestDream(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerSleepServiceLookupEvent lookup = new PlayerSleepServiceLookupEvent(player.getServer());
        MinecraftForge.EVENT_BUS.post(lookup);
        if (lookup.bridge().isEmpty()) {
            source.sendFailure(Component.literal("Echo Player Sleep service bridge was not found."));
            return 0;
        }
        var result = lookup.bridge().orElseThrow().arms().arm(
                new PlayerRef(player.getUUID()),
                new PlayerSleepScenarioId("echo_player_sleep:test/vanilla_bed_dream"),
                new PlayerSleepOwnerId("testmod:" + player.getUUID()),
                new PlayerSleepCommandId("testmod:" + player.serverLevel().getGameTime()));
        source.sendSuccess(() -> Component.literal("Test dream armed: " + result.outcome()), false);
        return result.diagnostics().isEmpty() ? 1 : 0;
    }
}
