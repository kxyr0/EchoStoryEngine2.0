package io.github.kxyr0.echostoryengine.blackspace;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echostoryengine.blackspace.api.BlackspaceScene;
import io.github.kxyr0.echostoryengine.blackspace.command.BlackspaceDebugCommand;
import io.github.kxyr0.echostoryengine.blackspace.network.BlackspaceNetwork;
import io.github.kxyr0.echostoryengine.blackspace.story.BlackspaceStoryExtensionFactory;
import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExtensionRegistrationEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(EchoBlackspace.MOD_ID)
public final class EchoBlackspace {
    public static final String MOD_ID = "echo_blackspace";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoBlackspace() {
        BlackspaceNetwork.register();
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);
        MinecraftForge.EVENT_BUS.addListener(this::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);
        MinecraftForge.EVENT_BUS.addListener(this::onBlockBreak);
        MinecraftForge.EVENT_BUS.addListener(this::onBlockPlace);
        MinecraftForge.EVENT_BUS.addListener(this::onBlockMultiPlace);
        MinecraftForge.EVENT_BUS.addListener(this::onFluidPlaceBlock);
        MinecraftForge.EVENT_BUS.addListener(this::onExplosionDetonate);
        MinecraftForge.EVENT_BUS.addListener(this::onStoryExtensionRegistration);
        LOGGER.info("Echo Blackspace started.");
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        new BlackspaceDebugCommand().register(event.getDispatcher());
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BlackspaceScene.onPlayerLoggedIn(player);
        }
    }

    private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BlackspaceScene.onPlayerLoggedOut(player);
        }
    }

    private void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            BlackspaceScene.tick(event.getServer());
        }
    }

    private void onServerStopping(ServerStoppingEvent event) {
        BlackspaceScene.onServerStopping(event.getServer());
    }

    private void onBlockBreak(BlockEvent.BreakEvent event) {
        if (isProtectedPlatform(event.getLevel(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    private void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (isProtectedPlatform(event.getLevel(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    private void onBlockMultiPlace(BlockEvent.EntityMultiPlaceEvent event) {
        if (event.getReplacedBlockSnapshots().stream()
                .anyMatch(snapshot -> isProtectedPlatform(snapshot.getLevel(), snapshot.getPos()))) {
            event.setCanceled(true);
        }
    }

    private void onFluidPlaceBlock(BlockEvent.FluidPlaceBlockEvent event) {
        if (isProtectedPlatform(event.getLevel(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    private void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        event.getAffectedBlocks().removeIf(pos -> BlackspaceScene.isProtectedPlatformBlock(level, pos));
    }

    private void onStoryExtensionRegistration(EchoStoryExtensionRegistrationEvent event) {
        event.registrar().register(new BlackspaceStoryExtensionFactory());
    }

    private boolean isProtectedPlatform(Object level, BlockPos pos) {
        return level instanceof ServerLevel serverLevel
                && BlackspaceScene.isProtectedPlatformBlock(serverLevel, pos);
    }
}
