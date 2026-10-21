package io.github.kxyr0.echoquests.forge.runtime;

import io.github.kxyr0.echoquests.api.runtime.QuestService;
import io.github.kxyr0.echoquests.core.catalog.QuestCatalog;
import io.github.kxyr0.echoquests.core.runtime.QuestStateMachine;
import io.github.kxyr0.echoquests.forge.persistence.QuestProgressSavedData;
import io.github.kxyr0.echoquests.forge.time.QuestTimeCoordinator;
import io.github.kxyr0.echotime.forge.api.event.EchoTimeOccurrenceBatchEvent;
import java.util.List;
import net.minecraft.server.MinecraftServer;

public final class QuestServerRuntime {
    private static final QuestServerRuntime INSTANCE = new QuestServerRuntime();
    private final QuestStateMachine machine = new QuestStateMachine();
    private final QuestService service = new ForgeQuestService(machine, this::dirty);
    private final QuestTimeCoordinator time = new QuestTimeCoordinator(machine, this::dirty);
    private QuestCatalog catalog = QuestCatalog.empty();
    private QuestProgressSavedData savedData;
    private MinecraftServer server;
    private long catalogRevision;

    private QuestServerRuntime() { }
    public static QuestServerRuntime instance() { return INSTANCE; }
    public QuestService service() { return service; }

    public synchronized boolean install(QuestCatalog replacement) {
        long nextRevision = catalogRevision + 1L;
        if (server != null && !time.publish(server, replacement, nextRevision)) return false;
        catalog = replacement;
        catalogRevision = nextRevision;
        machine.replaceCatalog(replacement);
        return true;
    }

    public synchronized void start(MinecraftServer server) {
        this.server = server;
        savedData = QuestProgressSavedData.get(server);
        machine.restore(savedData.progress());
        time.publish(server, catalog, catalogRevision);
    }

    public synchronized void stop() {
        dirty();
        savedData = null;
        server = null;
        time.clear();
        machine.restore(List.of());
    }

    public void occurrence(EchoTimeOccurrenceBatchEvent event) { time.occurrence(event); }

    private void dirty() {
        if (savedData != null) savedData.capture(machine.entries());
    }
}
