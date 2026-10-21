package io.github.kxyr0.echodialoguetriggeradapter.callback;

import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.service.DialogueService;
import io.github.kxyr0.echodialogue.api.service.DialogueStartRequest;
import io.github.kxyr0.echodialogue.forge.api.EchoDialogueServiceLookupEvent;
import io.github.kxyr0.echotrigger.api.callback.TriggerCallbackAction;
import io.github.kxyr0.echotrigger.api.callback.TriggerCallbackInvocation;
import io.github.kxyr0.echotrigger.forge.api.callback.TriggerCallbackInvocationEvent;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class DialogueTriggerCallbackListener {
    @SubscribeEvent
    public void onTriggerCallback(TriggerCallbackInvocationEvent event) {
        service().ifPresent(service -> startActions(service, event.invocation()));
    }

    private void startActions(DialogueService service, TriggerCallbackInvocation invocation) {
        invocation.actions().stream()
                .filter(this::startAction)
                .forEach(action -> start(service, invocation, action));
    }

    private void start(
            DialogueService service,
            TriggerCallbackInvocation invocation,
            TriggerCallbackAction action
    ) {
        dialogueId(action).ifPresent(dialogueId -> service.start(new DialogueStartRequest(
                sessionId(invocation, action), dialogueId, audienceId(invocation, action))));
    }

    private Optional<DialogueService> service() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return Optional.empty();
        }
        EchoDialogueServiceLookupEvent lookup = new EchoDialogueServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(lookup);
        return lookup.providers().stream().findFirst();
    }

    private boolean startAction(TriggerCallbackAction action) {
        return action.name().equals("Dialogue.Start")
                || action.name().equals("DialogueStart")
                || action.name().equals("StartDialogue");
    }

    private Optional<DialogueId> dialogueId(TriggerCallbackAction action) {
        Optional<String> raw = value(action, "dialogueId")
                .or(() -> value(action, "dialogue"))
                .or(() -> value(action, "id"))
                .or(() -> value(action, "arg0"));
        try {
            return raw.map(DialogueId::parse);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private DialogueSessionId sessionId(TriggerCallbackInvocation invocation, TriggerCallbackAction action) {
        String fallback = "trigger:" + invocation.instanceId().value() + ":" + invocation.kind().name()
                .toLowerCase(Locale.ROOT) + ":" + invocation.serverTick();
        return new DialogueSessionId(value(action, "sessionId")
                .or(() -> value(action, "session"))
                .orElse(fallback));
    }

    private AudienceId audienceId(TriggerCallbackInvocation invocation, TriggerCallbackAction action) {
        String owner = invocation.owner().value();
        String fallback = owner.startsWith("player:") ? owner.substring("player:".length()) : owner;
        return new AudienceId(value(action, "audienceId")
                .or(() -> value(action, "audience"))
                .orElse(fallback));
    }

    private Optional<String> value(TriggerCallbackAction action, String key) {
        return Optional.ofNullable(action.arguments().get(key)).filter(value -> !value.isBlank());
    }
}
