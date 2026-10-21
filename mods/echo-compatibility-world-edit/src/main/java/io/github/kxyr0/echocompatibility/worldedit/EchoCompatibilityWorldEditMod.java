package io.github.kxyr0.echocompatibility.worldedit;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(EchoCompatibilityWorldEditMod.MOD_ID)
public final class EchoCompatibilityWorldEditMod {
    public static final String MOD_ID = "echo_compatibility_world_edit";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final long DEFAULT_MAX_STATES_PER_BLOCK = 4_096L;
    private static final String LIMIT_PROPERTY = "echo.worldedit.maxStatesPerBlock";

    public EchoCompatibilityWorldEditMod() {
        LOGGER.info("Echo WorldEdit compatibility enabled; state prewarm limit is {} per block.",
                maxStatesPerBlock());
    }

    public static long maxStatesPerBlock() {
        long configured = Long.getLong(LIMIT_PROPERTY, DEFAULT_MAX_STATES_PER_BLOCK);
        return Math.max(1L, Math.min(configured, 1_000_000L));
    }
}
