package io.github.kxyr0.echomagic.core.gesture;

import io.github.kxyr0.echomagic.api.gesture.MagicGestureSymbol;
import io.github.kxyr0.echomagic.api.gesture.MagicTechnique;
import java.util.Optional;

public record MagicGestureSequence(Action action, Optional<MagicGestureSymbol> school,
                                   Optional<MagicTechnique> technique) {
    public MagicGestureSequence {
        if (action == null) throw new IllegalArgumentException("Gesture sequence action is required.");
        school = school == null ? Optional.empty() : school;
        technique = technique == null ? Optional.empty() : technique;
    }

    public static MagicGestureSequence resolve(Optional<MagicGestureSymbol> active,
                                               Optional<MagicGestureSymbol> recognized, boolean line) {
        return resolve(active, recognized, line ? Optional.of(MagicTechnique.LINE) : Optional.empty());
    }

    public static MagicGestureSequence resolve(Optional<MagicGestureSymbol> active,
                                               Optional<MagicGestureSymbol> recognized,
                                               Optional<MagicTechnique> technique) {
        if (active.isEmpty()) {
            if (technique.filter(value -> value == MagicTechnique.CIRCLE).isPresent()) {
                return new MagicGestureSequence(Action.SCHOOL_REQUIRED, Optional.empty(), technique);
            }
            return recognized.map(symbol -> new MagicGestureSequence(Action.SELECT_SCHOOL,
                    Optional.of(symbol), Optional.empty())).orElseGet(() -> new MagicGestureSequence(
                    Action.SCHOOL_REQUIRED, Optional.empty(), technique));
        }
        if (technique.filter(value -> value == MagicTechnique.CIRCLE).isPresent()) {
            return cast(active, technique.orElseThrow());
        }
        if (recognized.isPresent() && recognized.get() != active.get()) {
            return new MagicGestureSequence(Action.SELECT_SCHOOL, recognized, Optional.empty());
        }
        if (technique.isPresent()) return cast(active, technique.orElseThrow());
        if (recognized.isPresent()) {
            return new MagicGestureSequence(Action.SELECT_SCHOOL, recognized, Optional.empty());
        }
        return new MagicGestureSequence(Action.TECHNIQUE_UNRECOGNIZED, active, Optional.empty());
    }

    private static MagicGestureSequence cast(Optional<MagicGestureSymbol> school, MagicTechnique technique) {
        Action action = switch (technique) {
            case LINE -> Action.CAST_LINE;
            case ARC -> Action.CAST_ARC;
            case CIRCLE -> Action.CAST_CIRCLE;
            case HOOK -> Action.CAST_HOOK;
            case POINT -> Action.CAST_POINT;
            case WEAPON -> Action.CAST_WEAPON;
        };
        return new MagicGestureSequence(action, school, Optional.of(technique));
    }

    public enum Action {
        SELECT_SCHOOL,
        CAST_LINE,
        CAST_ARC,
        CAST_CIRCLE,
        CAST_HOOK,
        CAST_POINT,
        CAST_WEAPON,
        SCHOOL_REQUIRED,
        TECHNIQUE_UNRECOGNIZED
    }
}
