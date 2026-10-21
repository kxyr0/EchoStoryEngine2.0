package jdh.echo.animator.epicfight;

import com.mojang.logging.LogUtils;
import jdh.echo.animator.epicfight.client.ClientBootstrap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(EchoAnimatorEpicFightMod.MOD_ID)
public final class EchoAnimatorEpicFightMod {
    public static final String MOD_ID = "echo_animator_epic_fight";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoAnimatorEpicFightMod() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientBootstrap::register);
    }
}
