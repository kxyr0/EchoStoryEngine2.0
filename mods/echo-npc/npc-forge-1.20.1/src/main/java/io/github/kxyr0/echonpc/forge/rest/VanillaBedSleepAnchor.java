package io.github.kxyr0.echonpc.forge.rest;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echonpc.api.spatial.NpcPosition;
import io.github.kxyr0.echonpc.forge.entity.EchoNpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.slf4j.Logger;

final class VanillaBedSleepAnchor {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double BED_TOP_Y_OFFSET = 0.6875D;

    private VanillaBedSleepAnchor() {
    }

    static NpcPosition initial(BlockPos canonical) {
        return new NpcPosition(canonical.getX() + 0.5D, canonical.getY() + BED_TOP_Y_OFFSET,
                canonical.getZ() + 0.5D);
    }

    static void logVanilla(BlockPos canonical, Direction facing, EchoNpcEntity entity) {
        LOGGER.info("ENPC-REST-SLEEP-VANILLA actor={} form={} facing={} size={}x{} eye={} pos={}:{}:{} bed={}",
                entity.actorIdValue(), entity.formIdValue(), facing.getName(), entity.getBbWidth(),
                entity.getBbHeight(), entity.getEyeHeight(), entity.getX(), entity.getY(), entity.getZ(), canonical);
    }
}
