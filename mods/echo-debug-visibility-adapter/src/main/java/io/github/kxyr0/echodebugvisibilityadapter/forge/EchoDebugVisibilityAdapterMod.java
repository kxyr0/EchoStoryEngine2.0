package io.github.kxyr0.echodebugvisibilityadapter.forge;

import io.github.kxyr0.echodebugvisibilityadapter.forge.client.VisibilityDebugClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(EchoDebugVisibilityAdapterMod.MOD_ID)
public final class EchoDebugVisibilityAdapterMod {
    public static final String MOD_ID = "echo_debug_visibility_adapter";

    public EchoDebugVisibilityAdapterMod() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> VisibilityDebugClient::register);
    }
}
