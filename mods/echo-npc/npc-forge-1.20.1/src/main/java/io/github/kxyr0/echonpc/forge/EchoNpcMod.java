package io.github.kxyr0.echonpc.forge;

import io.github.kxyr0.echonpc.forge.composition.ForgeNpcCompositionRoot;
import io.github.kxyr0.echonpc.forge.registry.EchoNpcEntities;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Forge entrypoint and registration root for Echo NPC.
 */
@Mod(EchoNpcMod.MOD_ID)
public final class EchoNpcMod {
    public static final String MOD_ID = "echo_npc";
    private final ForgeNpcCompositionRoot root = new ForgeNpcCompositionRoot();

    public EchoNpcMod(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        EchoNpcEntities.register(modBus);
        root.register(modBus, MinecraftForge.EVENT_BUS);
    }
}
