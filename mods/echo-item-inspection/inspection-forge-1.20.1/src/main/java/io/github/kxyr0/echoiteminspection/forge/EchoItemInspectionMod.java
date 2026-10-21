package io.github.kxyr0.echoiteminspection.forge;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import io.github.kxyr0.echoiteminspection.client.bootstrap.InspectionClientBootstrap;

@Mod(EchoItemInspectionMod.ID)
public final class EchoItemInspectionMod {
    public static final String ID = "echo_item_inspection";

    public EchoItemInspectionMod() {
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.CLIENT, InspectionClientConfig.SPEC);
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> InspectionClientBootstrap::register);
    }
}
