package io.github.kxyr0.echodialoguetriggeradapter.execution;

import io.github.kxyr0.echodialoguetriggeradapter.bridge.DialogueTriggerWaitRegistration;
import io.github.kxyr0.echotrigger.api.model.TriggerResult;
import io.github.kxyr0.echotrigger.forge.api.TriggerService;
import java.util.ArrayList;
import java.util.List;

public final class DialogueTriggerCleanup {
    private final DialogueTriggerCommandExecutor commands = new DialogueTriggerCommandExecutor();

    public List<TriggerResult> cleanupInline(
            TriggerService service,
            List<DialogueTriggerWaitRegistration> registrations
    ) {
        List<TriggerResult> results = new ArrayList<>();
        registrations.stream()
                .filter(DialogueTriggerWaitRegistration::inline)
                .map(registration -> commands.stop(service, registration.key(), registration.instanceId()))
                .forEach(results::add);
        return List.copyOf(results);
    }
}
