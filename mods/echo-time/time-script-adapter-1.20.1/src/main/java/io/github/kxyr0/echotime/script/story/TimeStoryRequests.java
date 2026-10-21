package io.github.kxyr0.echotime.script.story;

import io.github.kxyr0.echotime.api.identity.TimeMutationId;
import io.github.kxyr0.echotime.api.identity.TimePauseOwner;
import io.github.kxyr0.echotime.api.identity.TimeWaitId;
import io.github.kxyr0.echotime.api.mutation.TimeRequestSource;
import io.github.kxyr0.echotime.script.action.TimeActionContext;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionRecoveryRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionStartRequest;
import java.util.Optional;
import java.util.Map;

final class TimeStoryRequests {
    private TimeStoryRequests() { }

    static TimeActionContext context(ActionStartRequest request) {
        return context(request.storyId().toString(), request.executionId().toString(),
                request.actionExecutionId().asString(), request.payload());
    }

    static TimeActionContext context(ActionRecoveryRequest request) {
        return context(request.storyId().toString(), request.executionId().toString(),
                request.actionExecutionId().asString(), request.payload());
    }

    private static TimeActionContext context(String story, String execution, String action,
                                             Map<String, String> payload) {
        String mutation = "story:" + action;
        TimeRequestSource source = new TimeRequestSource("echo_time", "story", action, execution,
                Optional.of(story), sourceLine(payload), "EchoScript Time action");
        return new TimeActionContext(new TimeMutationId(mutation), new TimePauseOwner("story:" + execution),
                new TimeWaitId("story:" + action), source);
    }

    private static Optional<Integer> sourceLine(Map<String, String> payload) {
        try {
            return Optional.ofNullable(payload.get("_sourceLine")).map(Integer::parseInt).filter(value -> value > 0);
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }
}
