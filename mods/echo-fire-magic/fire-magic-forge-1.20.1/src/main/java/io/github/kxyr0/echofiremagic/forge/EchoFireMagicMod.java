package io.github.kxyr0.echofiremagic.forge;

import io.github.kxyr0.echofiremagic.forge.blacksmith.BlacksmithHandRuntime;
import io.github.kxyr0.echofiremagic.forge.block.FireMagicBlocks;
import io.github.kxyr0.echomagic.forge.api.event.MagicLifecycleEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(EchoFireMagicMod.MOD_ID)
public final class EchoFireMagicMod {
    public static final String MOD_ID = "echo_fire_magic";

    private final BlacksmithHandRuntime blacksmithHand = new BlacksmithHandRuntime();

    public EchoFireMagicMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        FireMagicParticles.register(modBus);
        FireMagicBlocks.register(modBus);
        modBus.addListener(FireMagicBlocks::addToCreativeTab);
        MinecraftForge.EVENT_BUS.addListener(this::commands);
        MinecraftForge.EVENT_BUS.addListener(this::magicCast);
    }

    private void commands(RegisterCommandsEvent event) {
        new FireMagicCommands().register(event.getDispatcher());
    }

    private void magicCast(MagicLifecycleEvent event) {
        blacksmithHand.onMagicCast(event);
    }
}
