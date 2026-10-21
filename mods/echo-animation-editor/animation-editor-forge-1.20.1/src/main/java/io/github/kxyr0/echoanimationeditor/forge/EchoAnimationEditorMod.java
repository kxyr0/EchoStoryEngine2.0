package io.github.kxyr0.echoanimationeditor.forge;

import io.github.kxyr0.echoanimationeditor.forge.client.AnimatorEditorClientBootstrap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(EchoAnimationEditorMod.MOD_ID)
public final class EchoAnimationEditorMod {
    public static final String MOD_ID = "echo_animation_editor";

    public EchoAnimationEditorMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> AnimatorEditorClientBootstrap.bootstrap(modBus));
    }
}
