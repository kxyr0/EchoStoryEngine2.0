package io.github.kxyr0.echomagic.forge.gesture;

import io.github.kxyr0.echomagic.api.gesture.MagicGestureSymbol;
import io.github.kxyr0.echomagic.api.player.MagicPlayerRef;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class GestureSchoolRegistry {
    private final Map<MagicPlayerRef, MagicGestureSymbol> schools = new HashMap<>();

    public Optional<MagicGestureSymbol> school(MagicPlayerRef player) {
        return Optional.ofNullable(schools.get(player));
    }

    public void select(MagicPlayerRef player, MagicGestureSymbol school) { schools.put(player, school); }

    public void restore(MagicPlayerRef player, Optional<MagicGestureSymbol> school) {
        if (school.isPresent()) schools.put(player, school.orElseThrow());
        else schools.remove(player);
    }
}
