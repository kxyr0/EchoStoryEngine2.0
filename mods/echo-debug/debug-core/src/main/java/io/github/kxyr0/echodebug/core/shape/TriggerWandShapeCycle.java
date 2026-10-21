package io.github.kxyr0.echodebug.core.shape;

public final class TriggerWandShapeCycle {
    private TriggerWandShapeCycle() {
    }

    public static TriggerWandShapeMode next(TriggerWandShapeMode current, boolean reverse) {
        TriggerWandShapeMode[] values = TriggerWandShapeMode.values();
        int delta = reverse ? -1 : 1;
        int index = (current.ordinal() + delta + values.length) % values.length;
        return values[index];
    }
}
