package io.github.kxyr0.echonpcnavigationadapter.dialogue;

import io.github.kxyr0.echodialogue.api.service.DialogueService;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionView;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWait;
import io.github.kxyr0.echonavigation.forge.api.service.EchoNavigationServiceBridge;
import io.github.kxyr0.echonpc.forge.api.EchoNpcServiceBridge;
import net.minecraft.server.MinecraftServer;

public final class DialogueNavigationWaits {
    private final DialogueNavigationMoves moves =
            new DialogueNavigationMoves();

    private final DialogueNpcRestWaits restWaits =
            new DialogueNpcRestWaits();

    private final DialogueFormWaits formWaits =
            new DialogueFormWaits();

    private final DialogueSkinWaits skinWaits =
            new DialogueSkinWaits();

    public void move(
            MinecraftServer server,
            DialogueService service,
            DialogueSessionView session,
            DialogueExternalWait wait) {
        moves.move(server, service, session, wait);
    }

    public void moveArrival(
            MinecraftServer server,
            DialogueService service,
            DialogueSessionView session,
            DialogueExternalWait wait) {
        moves.moveArrival(server, service, session, wait);
    }

    public void stop(
            MinecraftServer server,
            DialogueService service,
            DialogueSessionView session,
            DialogueExternalWait wait) {
        moves.stop(server, service, session, wait);
    }

    public void setForm(
            MinecraftServer server,
            DialogueService service,
            DialogueSessionView session,
            DialogueExternalWait wait) {
        formWaits.setForm(server, service, session, wait);
    }

    public void changeSkin(
            MinecraftServer server,
            DialogueService service,
            DialogueSessionView session,
            DialogueExternalWait wait) {
        skinWaits.changeSkin(server, service, session, wait);
    }

    public void sleepAt(
            MinecraftServer server,
            DialogueService service,
            DialogueSessionView session,
            DialogueExternalWait wait) {
        restWaits.sleepAt(server, service, session, wait);
    }

    public void wakeUp(
            MinecraftServer server,
            DialogueService service,
            DialogueSessionView session,
            DialogueExternalWait wait) {
        restWaits.wakeUp(server, service, session, wait);
    }

    public void tick(MinecraftServer server) {
        EchoNavigationServiceBridge.lookup(server)
                .queryService()
                .ifPresent(service -> moves.tick(server, service));

        EchoNpcServiceBridge.lookup(server)
                .service()
                .ifPresent(service -> {
                    formWaits.tickForms(server, service);
                    skinWaits.tickSkins(server, service);
                });

        restWaits.tick(server);
    }

    public void close(MinecraftServer server) {
        moves.close(server);
        formWaits.close(server);
        skinWaits.clear();
        restWaits.close(server);
    }
}
