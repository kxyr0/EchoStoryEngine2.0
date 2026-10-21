package io.github.kxyr0.echomagic.core.gesture;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kxyr0.echomagic.api.gesture.MagicGestureSymbol;
import io.github.kxyr0.echomagic.api.gesture.MagicTechnique;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MagicGestureSequenceTest {
    @Test void firstSchoolSymbolOnlySelectsSchool() {
        var result = MagicGestureSequence.resolve(Optional.empty(), Optional.of(MagicGestureSymbol.FIRE), true);
        assertEquals(MagicGestureSequence.Action.SELECT_SCHOOL, result.action());
        assertEquals(MagicGestureSymbol.FIRE, result.school().orElseThrow());
    }

    @Test void repeatedFireStrokeBecomesLineTechnique() {
        var result = MagicGestureSequence.resolve(Optional.of(MagicGestureSymbol.FIRE),
                Optional.of(MagicGestureSymbol.FIRE), true);
        assertEquals(MagicGestureSequence.Action.CAST_LINE, result.action());
    }

    @Test void plainLineUsesRememberedSchool() {
        var result = MagicGestureSequence.resolve(Optional.of(MagicGestureSymbol.WATER), Optional.empty(), true);
        assertEquals(MagicGestureSequence.Action.CAST_LINE, result.action());
        assertEquals(MagicGestureSymbol.WATER, result.school().orElseThrow());
    }

    @Test void differentSchoolSymbolSwitchesBeforeCasting() {
        var result = MagicGestureSequence.resolve(Optional.of(MagicGestureSymbol.WATER),
                Optional.of(MagicGestureSymbol.FIRE), true);
        assertEquals(MagicGestureSequence.Action.SELECT_SCHOOL, result.action());
    }

    @Test void rejectsTechniqueWithoutSchoolAndScribbleWithSchool() {
        assertEquals(MagicGestureSequence.Action.SCHOOL_REQUIRED,
                MagicGestureSequence.resolve(Optional.empty(), Optional.empty(), true).action());
        assertEquals(MagicGestureSequence.Action.TECHNIQUE_UNRECOGNIZED,
                MagicGestureSequence.resolve(Optional.of(MagicGestureSymbol.AIR), Optional.empty(), false).action());
    }

    @Test void schoolSwitchWinsOverAmbiguousArcButHeldCircleUsesActiveSchool() {
        var switched = MagicGestureSequence.resolve(Optional.of(MagicGestureSymbol.WATER),
                Optional.of(MagicGestureSymbol.FIRE), Optional.of(MagicTechnique.ARC));
        assertEquals(MagicGestureSequence.Action.SELECT_SCHOOL, switched.action());
        var circle = MagicGestureSequence.resolve(Optional.of(MagicGestureSymbol.FIRE),
                Optional.of(MagicGestureSymbol.AIR), Optional.of(MagicTechnique.CIRCLE));
        assertEquals(MagicGestureSequence.Action.CAST_CIRCLE, circle.action());
        assertEquals(MagicGestureSymbol.FIRE, circle.school().orElseThrow());
    }

    @Test void largeCircleWithoutSelectedSchoolDoesNotBecomeAirSchool() {
        var result = MagicGestureSequence.resolve(Optional.empty(), Optional.of(MagicGestureSymbol.AIR),
                Optional.of(MagicTechnique.CIRCLE));
        assertEquals(MagicGestureSequence.Action.SCHOOL_REQUIRED, result.action());
        assertEquals(MagicTechnique.CIRCLE, result.technique().orElseThrow());
    }
}
