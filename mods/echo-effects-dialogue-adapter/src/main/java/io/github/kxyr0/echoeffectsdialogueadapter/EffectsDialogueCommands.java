package io.github.kxyr0.echoeffectsdialogueadapter;

final class EffectsDialogueCommands {
    static final String APPLY_TO_PLAYER = "echo_effects:apply_to_player";

    private EffectsDialogueCommands() {
    }

    static boolean supported(String waitType) {
        return APPLY_TO_PLAYER.equals(waitType);
    }
}
