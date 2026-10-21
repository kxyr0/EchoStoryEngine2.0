package io.github.kxyr0.echodialogue.echoscript.binding.node.name;

import java.util.Optional;

public final class ScreenCommandName {
    private ScreenCommandName() {
    }

    public static Optional<String> waitType(String rootName) {
        return switch (rootName) {
            case "Screen.SetBlack" -> Optional.of("echo_screen:set_black");
            default -> Optional.empty();
        };
    }
}
