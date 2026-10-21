package io.github.kxyr0.echodialoguetriggeradapter.execution;

import io.github.kxyr0.echodialoguetriggeradapter.bridge.DialogueInlineTriggerIds;
import io.github.kxyr0.echodialoguetriggeradapter.bridge.DialogueTriggerWaitRegistration;
import io.github.kxyr0.echodialoguetriggeradapter.bridge.DialogueTriggerWaitRequest;
import io.github.kxyr0.echotrigger.api.model.TriggerResult;
import io.github.kxyr0.echotrigger.forge.api.TriggerService;

public final class DialogueTriggerWaitExecutor {
    private final DialogueTriggerCommandExecutor commands = new DialogueTriggerCommandExecutor();
    private final DialogueInlineTriggerIds inlineIds = new DialogueInlineTriggerIds();

    public DialogueTriggerWaitRegistration wait(TriggerService service, DialogueTriggerWaitRequest request) {
        if (request.resourceTriggerId().isPresent()) {
            TriggerResult result = commands.start(service, request.key(),
                    request.resourceTriggerId().orElseThrow(), request.owner());
            return new DialogueTriggerWaitRegistration(request.key(), result.instanceId().orElseThrow(), false);
        }
        return new DialogueTriggerWaitRegistration(request.key(), inlineIds.instanceId(request.key()), true);
    }
}
