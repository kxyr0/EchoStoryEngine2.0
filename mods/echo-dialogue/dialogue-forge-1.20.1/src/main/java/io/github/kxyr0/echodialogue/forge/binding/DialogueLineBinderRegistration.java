package io.github.kxyr0.echodialogue.forge.binding;

import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.echoscript.api.DialogueLineArgumentBinder;
import io.github.kxyr0.echodialogue.forge.api.binding.EchoDialogueLineBinderRegistrationEvent;
import io.github.kxyr0.echodialogue.forge.diagnostic.ForgeDiagnosticSink;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraftforge.common.MinecraftForge;

/** Opens and validates one deterministic binder registration window. */
public final class DialogueLineBinderRegistration {
    private DialogueLineBinderRegistration() {
    }

    public static List<DialogueLineArgumentBinder> collect(ForgeDiagnosticSink sink) {
        EchoDialogueLineBinderRegistrationEvent event = new EchoDialogueLineBinderRegistrationEvent();
        MinecraftForge.EVENT_BUS.post(event);
        event.closeRegistration();
        List<DialogueLineArgumentBinder> accepted = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (DialogueLineArgumentBinder binder : event.binders()) {
            String name = binder.argumentName();
            if (name == null || !name.matches("[a-z][A-Za-z0-9_]*") || !names.add(name)) {
                sink.accept(DialogueDiagnostic.error("DIALOGUE_LINE_BINDER_INVALID",
                        DiagnosticCategory.SEMANTIC_VALIDATION,
                        "Line argument binder отклонён: invalid/duplicate name " + name + "."));
            } else {
                accepted.add(binder);
            }
        }
        return List.copyOf(accepted);
    }
}
