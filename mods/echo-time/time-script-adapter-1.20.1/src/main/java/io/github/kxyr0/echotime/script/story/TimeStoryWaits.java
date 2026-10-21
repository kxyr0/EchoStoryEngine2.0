package io.github.kxyr0.echotime.script.story;

import io.github.kxyr0.echotime.api.identity.TimeWaitId;
import io.github.kxyr0.echotime.forge.api.EchoTimeServiceAccess;
import io.github.kxyr0.echotime.forge.api.event.EchoTimeWaitCompletedEvent;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletion;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionSink;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionRecoveryRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionStartRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

final class TimeStoryWaits implements AutoCloseable {
    private final MinecraftServer server;
    private final Map<TimeWaitId, Pending> pending = new LinkedHashMap<>();

    TimeStoryWaits(MinecraftServer server) {
        this.server = server;
        MinecraftForge.EVENT_BUS.register(this);
    }

    void register(ActionStartRequest request, ActionCompletionSink sink, TimeWaitId waitId) {
        pending.put(waitId, new Pending(new ActionCompletion(request.storyId(), request.executionId(),
                request.actionExecutionId(), request.actionType(), ExternalActionResultCategory.SUCCESS, Map.of()), sink));
    }

    void register(ActionRecoveryRequest request, ActionCompletionSink sink, TimeWaitId waitId) {
        pending.put(waitId, new Pending(new ActionCompletion(request.storyId(), request.executionId(),
                request.actionExecutionId(), request.actionType(), ExternalActionResultCategory.SUCCESS, Map.of()), sink));
    }

    void cancel(String actionExecutionId) {
        pending.entrySet().removeIf(entry -> entry.getValue().completion().actionExecutionId().asString()
                .equals(actionExecutionId));
    }

    @SubscribeEvent
    public void onWaitCompleted(EchoTimeWaitCompletedEvent event) {
        if (event.server() != server) return;
        Pending value = pending.remove(event.waitId());
        if (value == null) return;
        EchoTimeServiceAccess.find(server).ifPresent(services -> services.waits().complete(event.waitId()));
        value.sink().complete(value.completion());
    }

    @Override public void close() { pending.clear(); MinecraftForge.EVENT_BUS.unregister(this); }

    private record Pending(ActionCompletion completion, ActionCompletionSink sink) { }
}
