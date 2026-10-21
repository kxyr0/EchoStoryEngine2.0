package io.github.kxyr0.echotriggertimeadapter.forge;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echotime.forge.api.event.EchoTimeOccurrenceBatchEvent;
import io.github.kxyr0.echotime.forge.api.event.EchoTimeResetEvent;
import io.github.kxyr0.echotriggertimeadapter.forge.delivery.TimeOccurrenceRouter;
import io.github.kxyr0.echotriggertimeadapter.forge.schedule.TimeSchedulePublisher;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(EchoTriggerTimeAdapterMod.MOD_ID)
public final class EchoTriggerTimeAdapterMod {
    public static final String MOD_ID = "echo_trigger_time_adapter";
    private static final Logger LOGGER = LogUtils.getLogger();
    private final TimeSchedulePublisher schedules = new TimeSchedulePublisher();
    private MinecraftServer server;
    private TimeOccurrenceRouter router;

    public EchoTriggerTimeAdapterMod() { MinecraftForge.EVENT_BUS.register(this); }

    @SubscribeEvent
    public void started(ServerStartedEvent event) {
        server = event.getServer();
        router = new TimeOccurrenceRouter(server);
        schedules.refresh(server, router.occurrenceGeneration());
        router.recoverPending();
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() != server
                || event.getServer().getTickCount() % 20 != 0) return;
        schedules.refresh(server, router.occurrenceGeneration());
        router.recoverPending();
    }

    @SubscribeEvent
    public void reset(EchoTimeResetEvent event) {
        if (event.server() != server || router == null) return;
        long generation = router.advanceOccurrenceGeneration();
        schedules.refresh(server, generation);
        LOGGER.info("[{}] Echo Time reset opened Time.At replay generation {} at day={} time={}.",
                TimeOccurrenceRouter.REPLAY_SUPPRESSED_CODE, generation,
                event.instant().day().value(), event.instant().timeOfDay().formatHHmm());
    }

    @SubscribeEvent
    public void occurrences(EchoTimeOccurrenceBatchEvent event) {
        if (event.server() == server && router != null) router.routeAll(event.occurrences(), schedules.index());
    }

    @SubscribeEvent
    public void stopping(ServerStoppingEvent event) {
        if (event.getServer() == server) { router = null; server = null; }
    }
}
