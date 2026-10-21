package io.github.kxyr0.echonpcinteraction.forge;

import io.github.kxyr0.echonpcinteraction.forge.composition.ForgeNpcInteractionRoot;
import io.github.kxyr0.echonpcinteraction.forge.seat.EchoNpcInteractionEntities;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(EchoNpcInteractionMod.MOD_ID)
public final class EchoNpcInteractionMod {
    public static final String MOD_ID = "echo_npc_interaction";

    public EchoNpcInteractionMod() {
        EchoNpcInteractionEntities.register(FMLJavaModLoadingContext.get().getModEventBus());
        new ForgeNpcInteractionRoot().register(
                FMLJavaModLoadingContext.get().getModEventBus(),
                MinecraftForge.EVENT_BUS);
    }
}
