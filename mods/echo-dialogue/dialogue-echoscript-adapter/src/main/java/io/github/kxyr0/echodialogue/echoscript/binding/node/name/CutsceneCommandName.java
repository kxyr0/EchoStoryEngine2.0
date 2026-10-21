package io.github.kxyr0.echodialogue.echoscript.binding.node.name;

import java.util.Optional;

public final class CutsceneCommandName {
    private CutsceneCommandName() {
    }

    public static Optional<String> action(String rootName) {
        return switch (rootName) {
            case "Cutscene.Start" -> Optional.of("start");
            case "Cutscene.Play" -> Optional.of("play");
            case "Cutscene.Wait" -> Optional.of("wait");
            case "Cutscene.WaitPrepared" -> Optional.of("wait_prepared");
            case "Cutscene.Stop" -> Optional.of("stop");
            default -> Optional.empty();
        };
    }
}
