package io.github.kxyr0.echonpcbackpack.forge.client;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import java.util.Optional;

public final class BackpackDebugState {
    private static volatile Optional<ActorId> actor = Optional.empty();

    private BackpackDebugState() {
    }

    public static void set(Optional<ActorId> value) {
        actor = value;
    }

    public static boolean enabled(ActorId value) {
        return actor.filter(value::equals).isPresent();
    }
}
