package io.github.kxyr0.echoplayervitalsdialogueadapter;

import java.util.Set;

final class PlayerVitalsDialogueCommands {
    static final String SET_FATIGUE = "echo_player_vitals:set_fatigue";
    static final String ADD_FATIGUE = "echo_player_vitals:add_fatigue";
    static final String REDUCE_FATIGUE = "echo_player_vitals:reduce_fatigue";
    static final String RESTORE_FATIGUE = "echo_player_vitals:restore_fatigue";

    private static final Set<String> SUPPORTED = Set.of(
            SET_FATIGUE,
            ADD_FATIGUE,
            REDUCE_FATIGUE,
            RESTORE_FATIGUE
    );

    private PlayerVitalsDialogueCommands() {
    }

    static boolean supported(String waitType) {
        return SUPPORTED.contains(waitType);
    }
}
