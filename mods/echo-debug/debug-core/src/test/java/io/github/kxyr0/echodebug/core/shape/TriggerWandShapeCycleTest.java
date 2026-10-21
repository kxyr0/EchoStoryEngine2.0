package io.github.kxyr0.echodebug.core.shape;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TriggerWandShapeCycleTest {
    @Test
    void cyclesForwardWithVOrder() {
        assertEquals(TriggerWandShapeMode.SPHERE,
                TriggerWandShapeCycle.next(TriggerWandShapeMode.BOX, false));
        assertEquals(TriggerWandShapeMode.CYLINDER,
                TriggerWandShapeCycle.next(TriggerWandShapeMode.SPHERE, false));
        assertEquals(TriggerWandShapeMode.POLYGON,
                TriggerWandShapeCycle.next(TriggerWandShapeMode.CYLINDER, false));
        assertEquals(TriggerWandShapeMode.SEAT,
                TriggerWandShapeCycle.next(TriggerWandShapeMode.POLYGON, false));
        assertEquals(TriggerWandShapeMode.BOX,
                TriggerWandShapeCycle.next(TriggerWandShapeMode.SEAT, false));
    }

    @Test
    void cyclesBackwardWithShiftV() {
        assertEquals(TriggerWandShapeMode.SEAT,
                TriggerWandShapeCycle.next(TriggerWandShapeMode.BOX, true));
    }
}
