package io.github.kxyr0.echoanimation.forge.client.dialogue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.eventbus.api.Event;

/** Reflection-only accessors isolating the optional Echo Dialogue API. */
final class DialogueEventAccess {
    private DialogueEventAccess() {
    }

    record Open(Method message) {
        static Open create(Class<?> type) throws NoSuchMethodException {
            return new Open(type.getMethod("message"));
        }

        Optional<String> visualId(Event event) throws ReflectiveOperationException {
            Object payload = invoke(message, event);
            Optional<?> visual = (Optional<?>) invoke(payload.getClass().getMethod("visual"), payload);
            if (visual.isEmpty()) {
                return Optional.empty();
            }
            Object value = visual.orElseThrow();
            return Optional.of((String) invoke(value.getClass().getMethod("id"), value));
        }

        Optional<String> speakerId(Event event) throws ReflectiveOperationException {
            Object payload = invoke(message, event);
            try {
                Optional<?> speaker = (Optional<?>) invoke(payload.getClass().getMethod("speakerId"), payload);
                return speaker.map(Object::toString);
            } catch (NoSuchMethodException exception) {
                return Optional.empty();
            }
        }

        Optional<String> faceAnimation(Event event) throws ReflectiveOperationException {
            return cue(event, "faceAnimation");
        }

        boolean faceAnimationClear(Event event) throws ReflectiveOperationException {
            return cue(event, "faceAnimationClear").isPresent();
        }

        boolean faceAnimationStop(Event event) throws ReflectiveOperationException {
            return cue(event, "faceAnimationStop").isPresent();
        }

        private Optional<String> cue(Event event, String name) throws ReflectiveOperationException {
            Object payload = invoke(message, event);
            for (Object cue : (List<?>) invoke(payload.getClass().getMethod("cues"), payload)) {
                if (name.equals((String) invoke(cue.getClass().getMethod("name"), cue))) {
                    return Optional.of((String) invoke(cue.getClass().getMethod("value"), cue));
                }
            }
            return Optional.empty();
        }
    }

    record Visual(Method graphics, Method visualId, Method x, Method y, Method width, Method height) {
        static Visual create(Class<?> type) throws NoSuchMethodException {
            return new Visual(type.getMethod("graphics"), type.getMethod("visualId"), type.getMethod("x"),
                    type.getMethod("y"), type.getMethod("width"), type.getMethod("height"));
        }

        GuiGraphics graphics(Event event) throws ReflectiveOperationException {
            return (GuiGraphics) invoke(graphics, event);
        }

        String visualId(Event event) throws ReflectiveOperationException {
            return (String) invoke(visualId, event);
        }

        DialogueVisualFaceLayerRenderer.Bounds bounds(Event event) throws ReflectiveOperationException {
            return new DialogueVisualFaceLayerRenderer.Bounds(value(x, event), value(y, event),
                    value(width, event), value(height, event));
        }

        private int value(Method method, Event event) throws ReflectiveOperationException {
            return ((Integer) invoke(method, event)).intValue();
        }
    }

    private static Object invoke(Method method, Object target) throws ReflectiveOperationException {
        try {
            return method.invoke(target);
        } catch (InvocationTargetException exception) {
            throw new ReflectiveOperationException(exception.getCause());
        }
    }
}
