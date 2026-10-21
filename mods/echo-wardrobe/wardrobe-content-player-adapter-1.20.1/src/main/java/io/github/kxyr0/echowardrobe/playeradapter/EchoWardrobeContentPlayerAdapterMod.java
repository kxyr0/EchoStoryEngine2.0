package io.github.kxyr0.echowardrobe.playeradapter;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoplayer.forge.api.appearance.PlayerAppearanceExtensionRegistryBridge;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Registers Wardrobe as a neutral Echo Content Player appearance contributor.
 */
@Mod(EchoWardrobeContentPlayerAdapterMod.MOD_ID)
public final class EchoWardrobeContentPlayerAdapterMod {
    public static final String MOD_ID = "echo_wardrobe_content_player_adapter";
    private static final Logger LOGGER = LogUtils.getLogger();
    private boolean registered;

    public EchoWardrobeContentPlayerAdapterMod() {
        register();
    }

    private synchronized void register() {
        if (registered) {
            return;
        }
        PlayerAppearanceExtensionRegistryBridge.register(new WardrobePlayerAppearanceContribution());
        registered = true;
        LOGGER.info("event=wardrobe_content_player_adapter_registered");
    }
}
