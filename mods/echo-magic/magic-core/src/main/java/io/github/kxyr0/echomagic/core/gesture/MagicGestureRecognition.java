package io.github.kxyr0.echomagic.core.gesture;

import io.github.kxyr0.echomagic.api.gesture.MagicGestureSymbol;
import java.util.Optional;

public record MagicGestureRecognition(Optional<MagicGestureSymbol> symbol, double confidence) {
    public MagicGestureRecognition {
        symbol = symbol == null ? Optional.empty() : symbol;
        if (!Double.isFinite(confidence) || confidence < 0.0D || confidence > 1.0D) {
            throw new IllegalArgumentException("Gesture confidence must be between zero and one.");
        }
    }

    public static MagicGestureRecognition recognized(MagicGestureSymbol symbol, double confidence) {
        return new MagicGestureRecognition(Optional.of(symbol), confidence);
    }

    public static MagicGestureRecognition unknown() {
        return new MagicGestureRecognition(Optional.empty(), 0.0D);
    }
}
