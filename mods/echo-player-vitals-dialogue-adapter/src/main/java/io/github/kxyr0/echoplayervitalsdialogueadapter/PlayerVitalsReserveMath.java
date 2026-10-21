package io.github.kxyr0.echoplayervitalsdialogueadapter;

final class PlayerVitalsReserveMath {
    private PlayerVitalsReserveMath() {
    }

    static int rawFatigueForReserve(int reserve, int maximum) {
        int clampedReserve = Math.max(0, Math.min(reserve, maximum));
        return maximum - clampedReserve;
    }
}
