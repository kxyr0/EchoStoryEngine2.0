package io.github.kxyr0.echotime.script;

import io.github.kxyr0.echodialogue.forge.api.wait.DialogueExternalWaitDispatchEvent;
import io.github.kxyr0.echotime.forge.api.event.EchoTimeWaitCompletedEvent;
import io.github.kxyr0.echotime.script.dialogue.TimeDialogueController;
import io.github.kxyr0.echotime.script.story.TimeStoryExtensionFactory;
import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExtensionRegistrationEvent;
import io.github.kxyr0.echostoryengine.story.extension.forge.lifecycle.EchoStoryExecutionTerminalEvent;
import io.github.kxyr0.echotime.api.identity.TimeMutationId;
import io.github.kxyr0.echotime.api.identity.TimePauseOwner;
import io.github.kxyr0.echotime.forge.api.EchoTimeServiceAccess;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class TimeScriptIntegration {
    private final Map<MinecraftServer, TimeDialogueController> dialogues = new IdentityHashMap<>();

    public TimeScriptIntegration() { MinecraftForge.EVENT_BUS.register(this); }

    @SubscribeEvent
    public void registerStory(EchoStoryExtensionRegistrationEvent event) {
        event.registrar().register(new TimeStoryExtensionFactory());
    }

    @SubscribeEvent
    public void dialogueWait(DialogueExternalWaitDispatchEvent event) {
        MinecraftServer server = event.player().map(net.minecraft.server.level.ServerPlayer::getServer).orElseGet(() ->
                net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer());
        if (server != null) dialogues.computeIfAbsent(server, TimeDialogueController::new).dispatch(event);
    }

    @SubscribeEvent
    public void timeWait(EchoTimeWaitCompletedEvent event) {
        TimeDialogueController controller = dialogues.get(event.server());
        if (controller != null) controller.waitCompleted(event);
    }

    @SubscribeEvent
    public void storyTerminal(EchoStoryExecutionTerminalEvent event) {
        String execution = event.executionId().toString();
        EchoTimeServiceAccess.find(event.server()).ifPresent(time -> time.mutations().resume(
                new TimePauseOwner("story:" + execution),
                new TimeMutationId("story-terminal:" + execution)));
    }

    @SubscribeEvent
    public void stopping(ServerStoppingEvent event) {
        TimeDialogueController controller = dialogues.remove(event.getServer());
        if (controller != null) controller.close();
    }
}
