package io.github.kxyr0.echovoidmagic.forge;

import io.github.kxyr0.echomagic.forge.api.event.MagicLifecycleEvent;
import io.github.kxyr0.echovoidmagic.forge.teleport.VoidStepRuntime;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(EchoVoidMagicMod.MOD_ID)
public final class EchoVoidMagicMod {
    public static final String MOD_ID = "echo_void_magic";
    private final VoidStepRuntime voidStep = new VoidStepRuntime();
    public EchoVoidMagicMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        VoidMagicParticles.register(modBus);
        MinecraftForge.EVENT_BUS.addListener(this::commands);
        MinecraftForge.EVENT_BUS.addListener(this::magicCast);
    }
    private void commands(RegisterCommandsEvent event) { new VoidMagicCommands().register(event.getDispatcher()); }
    private void magicCast(MagicLifecycleEvent event) { voidStep.onMagicCast(event); }
}
