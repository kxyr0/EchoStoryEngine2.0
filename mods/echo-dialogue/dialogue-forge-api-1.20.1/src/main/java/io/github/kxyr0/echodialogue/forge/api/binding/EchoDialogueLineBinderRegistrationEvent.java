package io.github.kxyr0.echodialogue.forge.api.binding;

import io.github.kxyr0.echodialogue.echoscript.api.DialogueLineArgumentBinder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraftforge.eventbus.api.Event;

/** Registration window for external EchoScript Line argument binders. */
public final class EchoDialogueLineBinderRegistrationEvent extends Event {
    private final List<DialogueLineArgumentBinder> binders = new ArrayList<>();
    private boolean closed;

    public void register(DialogueLineArgumentBinder binder) {
        if (closed) {
            throw new IllegalStateException("Регистрация Line argument binders уже закрыта.");
        }
        binders.add(Objects.requireNonNull(binder, "binder"));
    }

    public List<DialogueLineArgumentBinder> binders() {
        return List.copyOf(binders);
    }

    public void closeRegistration() {
        closed = true;
    }
}
