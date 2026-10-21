package io.github.kxyr0.echoplayersleep.script;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.ActionHandlerCapabilities;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.ActionHandlerDescriptor;
import java.util.List;

public final class SleepActionCatalog {
    public static final NamespacedId ARM = NamespacedId.of("echo_player_sleep:arm");
    public static final NamespacedId DISARM = NamespacedId.of("echo_player_sleep:disarm");
    public static final NamespacedId CLOSE_EYES = NamespacedId.of("echo_player_sleep:close_eyes");
    public static final NamespacedId WAIT_UNTIL_CLOSED = NamespacedId.of("echo_player_sleep:wait_until_closed");
    public static final NamespacedId ENTER_DREAM = NamespacedId.of("echo_player_sleep:enter_dream");
    public static final NamespacedId OPEN_EYES = NamespacedId.of("echo_player_sleep:open_eyes");
    public static final NamespacedId WAIT_UNTIL_OPEN = NamespacedId.of("echo_player_sleep:wait_until_open");
    public static final NamespacedId WAIT = NamespacedId.of("echo_player_sleep:wait");
    public static final NamespacedId RETURN_TO_BED = NamespacedId.of("echo_player_sleep:return_to_bed");
    public static final NamespacedId COMPLETE_NIGHT = NamespacedId.of("echo_player_sleep:complete_night");
    public static final NamespacedId COMPLETE = NamespacedId.of("echo_player_sleep:complete");
    public static final NamespacedId CANCEL = NamespacedId.of("echo_player_sleep:cancel");
    public static final NamespacedId IS_ACTIVE = NamespacedId.of("echo_player_sleep:is_active");
    public static final NamespacedId IS_ARMED = NamespacedId.of("echo_player_sleep:is_armed");
    public static final NamespacedId IS_EYES_CLOSED = NamespacedId.of("echo_player_sleep:is_eyes_closed");

    private SleepActionCatalog() {
    }

    public static List<NamespacedId> all() {
        return List.of(ARM, DISARM, CLOSE_EYES, WAIT_UNTIL_CLOSED, ENTER_DREAM, OPEN_EYES,
                WAIT_UNTIL_OPEN, WAIT, RETURN_TO_BED, COMPLETE_NIGHT, COMPLETE, CANCEL,
                IS_ACTIVE, IS_ARMED, IS_EYES_CLOSED);
    }

    public static List<ActionHandlerDescriptor> descriptors() {
        return all().stream()
                .map(id -> new ActionHandlerDescriptor(id, ActionHandlerCapabilities.recoverableAsync()))
                .toList();
    }
}
