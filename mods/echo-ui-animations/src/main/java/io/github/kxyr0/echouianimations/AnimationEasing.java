package io.github.kxyr0.echouianimations;

/**
 * Stable easing functions used by built-in presets.
 */
public enum AnimationEasing {
    LINEAR {
        @Override
        public float apply(float progress) {
            return progress;
        }
    },
    EASE_OUT_CUBIC {
        @Override
        public float apply(float progress) {
            float inverted = 1.0F - progress;
            return 1.0F - inverted * inverted * inverted;
        }
    },
    EASE_IN_OUT_CUBIC {
        @Override
        public float apply(float progress) {
            if (progress < 0.5F) {
                return 4.0F * progress * progress * progress;
            }
            float shifted = -2.0F * progress + 2.0F;
            return 1.0F - shifted * shifted * shifted / 2.0F;
        }
    },
    EASE_OUT_BACK {
        @Override
        public float apply(float progress) {
            float c1 = 1.70158F;
            float c3 = c1 + 1.0F;
            float shifted = progress - 1.0F;
            return 1.0F + c3 * shifted * shifted * shifted + c1 * shifted * shifted;
        }
    };

    public abstract float apply(float progress);
}
