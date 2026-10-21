package io.github.kxyr0.echodebugvisibilityadapter.forge.client;

import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchId;
import java.util.Optional;

public final class VisibilityDebugSettings {
    private boolean player;
    private boolean all;
    private Optional<VisibilityWatchId> watch = Optional.empty();

    public void player(boolean enabled) {
        player = enabled;
    }

    public void all(boolean enabled) {
        all = enabled;
        if (!enabled) {
            watch = Optional.empty();
        }
    }

    public void watch(Optional<VisibilityWatchId> value) {
        watch = value;
        all = false;
    }

    public boolean player() {
        return player || all;
    }

    public boolean all() {
        return all;
    }

    public Optional<VisibilityWatchId> watch() {
        return watch;
    }

    public boolean active() {
        return player || all || watch.isPresent();
    }

    public String status() {
        return "player=" + player + ", all=" + all
                + ", watch=" + watch.map(VisibilityWatchId::value).orElse("off");
    }
}
