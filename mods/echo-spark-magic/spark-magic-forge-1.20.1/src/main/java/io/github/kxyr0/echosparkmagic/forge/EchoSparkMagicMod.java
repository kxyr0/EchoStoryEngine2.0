package io.github.kxyr0.echosparkmagic.forge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(EchoSparkMagicMod.MOD_ID)
public final class EchoSparkMagicMod {
    public static final String MOD_ID = "echo_spark_magic";

    public EchoSparkMagicMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        SparkMagicParticles.register(modBus);
        MinecraftForge.EVENT_BUS.register(new SparkMagicMovementTrail());
        MinecraftForge.EVENT_BUS.addListener(this::commands);
        MinecraftForge.EVENT_BUS.addListener(this::clonePlayer);
    }

    private void commands(RegisterCommandsEvent event) {
        new SparkMagicCommands().register(event.getDispatcher());
    }

    private void clonePlayer(PlayerEvent.Clone event) {
        SparkMagicPlayerState.copyAfterClone(event);
    }
}

