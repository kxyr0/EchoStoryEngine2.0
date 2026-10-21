package io.github.kxyr0.echobackgroundnpc.testmod;

import io.github.kxyr0.echobackgroundnpc.forge.api.location.BackgroundLocationResolution;
import io.github.kxyr0.echobackgroundnpc.forge.api.location.BackgroundLocationResolverEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod("echo_background_npc_testmod")
public final class EchoBackgroundNpcTestMod {
    public EchoBackgroundNpcTestMod() {
        MinecraftForge.EVENT_BUS.addListener(BenchmarkScene::commands);
        MinecraftForge.EVENT_BUS.addListener(ServerTickBenchmark::tick);
        MinecraftForge.EVENT_BUS.addListener(this::locations);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientFrameBenchmark::register);
    }

    private void locations(BackgroundLocationResolverEvent event) { event.register((server, id) -> {
        BlockPos spawn = server.overworld().getSharedSpawnPos();
        BlockPos offset = switch (id.value()) {
            case "jdh:dorm_mira" -> spawn.offset(-14, 0, 0);
            case "jdh:dorm_anton" -> spawn.offset(-12, 0, 0);
            case "jdh:university_gate" -> spawn.offset(0, 0, 12);
            case "jdh:hallway_window" -> spawn.offset(5, 0, 12);
            case "jdh:room_204" -> spawn.offset(12, 0, 12);
            case "jdh:cafeteria" -> spawn.offset(18, 0, 5);
            default -> null;
        };
        return offset == null ? BackgroundLocationResolution.missing(id)
                : BackgroundLocationResolution.found(id, ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION, Level.OVERWORLD.location()), offset);
    }); }
}
