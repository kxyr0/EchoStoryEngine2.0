package io.github.kxyr0.echoquests.forge;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoquests.forge.api.QuestServiceLookupEvent;
import io.github.kxyr0.echoquests.forge.command.QuestCommands;
import io.github.kxyr0.echoquests.forge.resource.QuestReloadListener;
import io.github.kxyr0.echoquests.forge.runtime.QuestServerRuntime;
import io.github.kxyr0.echotime.forge.api.event.EchoTimeOccurrenceBatchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(EchoQuestsMod.MOD_ID)
public final class EchoQuestsMod {
    public static final String MOD_ID = "echo_quests";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EchoQuestsMod() {
        var bus = MinecraftForge.EVENT_BUS;
        bus.addListener(this::reloadListeners);
        bus.addListener(this::serverStarted);
        bus.addListener(this::serverStopped);
        bus.addListener(this::commands);
        bus.addListener(this::serviceLookup);
        bus.addListener(this::timeOccurrences);
    }

    private void reloadListeners(AddReloadListenerEvent event) {
        event.addListener(new QuestReloadListener(LOGGER));
    }
    private void serverStarted(ServerStartedEvent event) { QuestServerRuntime.instance().start(event.getServer()); }
    private void serverStopped(ServerStoppedEvent event) { QuestServerRuntime.instance().stop(); }
    private void commands(RegisterCommandsEvent event) {
        QuestCommands.register(event.getDispatcher(), () -> QuestServerRuntime.instance().service());
    }
    private void serviceLookup(QuestServiceLookupEvent event) {
        event.provide(QuestServerRuntime.instance().service());
    }
    private void timeOccurrences(EchoTimeOccurrenceBatchEvent event) {
        QuestServerRuntime.instance().occurrence(event);
    }
}
