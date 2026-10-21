package jdh.echo.tacz.epicfight;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(EchoTaczEpicFight.MOD_ID)
public final class EchoTaczEpicFight {
    public static final String MOD_ID = "echo_tacz_epicfight";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoTaczEpicFight() {
        LOGGER.info("Loaded Echo TACZ Epic Fight Adapter");
        logDetectedVersion("tacz");
        logDetectedVersion("epicfight");
    }

    private static void logDetectedVersion(String modId) {
        ModList.get().getModContainerById(modId).ifPresentOrElse(
                container -> LOGGER.info("Detected {} {}", modId, container.getModInfo().getVersion()),
                () -> LOGGER.warn("Required mod {} is not present in ModList", modId));
    }
}
