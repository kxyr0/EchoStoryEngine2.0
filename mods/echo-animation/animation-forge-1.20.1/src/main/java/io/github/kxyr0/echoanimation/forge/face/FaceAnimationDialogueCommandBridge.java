package io.github.kxyr0.echoanimation.forge.face;

import com.mojang.logging.LogUtils;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

public final class FaceAnimationDialogueCommandBridge {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DIALOGUE_MOD_ID = "echo_dialogue";
    private static final String FACE_ANIMATION_WAIT_TYPE = "echo_animation:set_face_animation";
    private static final String CLEAR_FACE_ANIMATION_WAIT_TYPE = "echo_animation:clear_face_animation";
    private static final String CLEAR_ALL_FACE_ANIMATIONS_WAIT_TYPE = "echo_animation:clear_all_face_animations";
    private static final String SET_BLINK_TIMING_WAIT_TYPE = "echo_animation:set_blink_timing";
    private static final String CLEAR_BLINK_TIMING_WAIT_TYPE = "echo_animation:clear_blink_timing";

    private final ServerFaceAnimationState state;
    private final Supplier<Optional<MinecraftServer>> server;
    private final DialogueReflectionAccess access;

    private FaceAnimationDialogueCommandBridge(
            ServerFaceAnimationState state,
            Supplier<Optional<MinecraftServer>> server,
            DialogueReflectionAccess access
    ) {
        this.state = state;
        this.server = server;
        this.access = access;
    }

    public static void register(
            IEventBus forgeBus,
            ServerFaceAnimationState state,
            Supplier<Optional<MinecraftServer>> server
    ) {
        if (!ModList.get().isLoaded(DIALOGUE_MOD_ID)) {
            return;
        }
        try {
            DialogueReflectionAccess access = DialogueReflectionAccess.create();
            FaceAnimationDialogueCommandBridge bridge =
                    new FaceAnimationDialogueCommandBridge(state, server, access);
            bridge.subscribe(forgeBus);
            LOGGER.info("Echo Animation dialogue face command bridge registered.");
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("Echo Animation dialogue face command bridge is unavailable: {}", exception.toString());
        }
    }

    private void handle(Event event) {
        try {
            DialogueReflectionAccess.Wait wait = access.wait(event);
            if (isHandled(wait.type())) {
                runCommand(event, wait);
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.warn("Failed to handle Echo Animation dialogue face command.", exception);
        }
    }

    private void runCommand(Event event, DialogueReflectionAccess.Wait wait) throws ReflectiveOperationException {
        Object session = access.session(event);
        MinecraftServer currentServer = currentServer(event);
        try {
            switch (wait.type()) {
                case FACE_ANIMATION_WAIT_TYPE -> state.set(currentServer,
                        FaceAnimationCommandPayload.decode(wait.payload()));
                case CLEAR_FACE_ANIMATION_WAIT_TYPE -> state.clear(currentServer,
                        FaceAnimationCommandPayload.decodeSubject(wait.payload()));
                case CLEAR_ALL_FACE_ANIMATIONS_WAIT_TYPE -> state.clearAll(currentServer);
                case SET_BLINK_TIMING_WAIT_TYPE -> state.setBlinkTiming(currentServer,
                        BlinkTimingCommandPayload.decode(wait.payload()));
                case CLEAR_BLINK_TIMING_WAIT_TYPE -> state.clearBlinkTiming(currentServer,
                        BlinkTimingCommandPayload.decodeSubject(wait.payload()));
                default -> { }
            }
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("Invalid {} payload for {}: {}", wait.type(), wait.key(), exception.getMessage());
        }
        access.resume(currentServer, session, wait);
    }

    private boolean isHandled(String waitType) {
        return FACE_ANIMATION_WAIT_TYPE.equals(waitType)
                || CLEAR_FACE_ANIMATION_WAIT_TYPE.equals(waitType)
                || CLEAR_ALL_FACE_ANIMATIONS_WAIT_TYPE.equals(waitType)
                || SET_BLINK_TIMING_WAIT_TYPE.equals(waitType)
                || CLEAR_BLINK_TIMING_WAIT_TYPE.equals(waitType);
    }

    private MinecraftServer currentServer(Event event) throws ReflectiveOperationException {
        return access.player(event)
                .map(player -> player.getServer())
                .or(() -> server.get())
                .orElse(null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void subscribe(IEventBus forgeBus) {
        forgeBus.addListener(EventPriority.NORMAL, false, (Class) access.waitEventClass(),
                (Consumer<Event>) this::handle);
    }
}
