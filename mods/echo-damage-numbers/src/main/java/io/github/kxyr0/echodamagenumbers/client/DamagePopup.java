package io.github.kxyr0.echodamagenumbers.client;

import java.util.Locale;

final class DamagePopup {
    static final int LIFETIME_TICKS = 40;
    final double x;
    final double y;
    final double z;
    final String text;
    final int color;
    final float waveDirection;
    int age;

    DamagePopup(double x, double y, double z, float amount, boolean waveRight) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.text = format(amount);
        this.color = color(amount);
        this.waveDirection = waveRight ? 1.0F : -1.0F;
    }

    float progress(float partialTick) {
        return Math.min(1.0F, (age + partialTick) / LIFETIME_TICKS);
    }

    private static String format(float amount) {
        float rounded = Math.round(amount);
        return Math.abs(amount - rounded) < 0.05F
                ? Integer.toString(Math.round(amount)) : String.format(Locale.ROOT, "%.1f", amount);
    }

    private static int color(float amount) {
        if (amount >= 10.0F) return 0xFF4A3D;
        if (amount >= 5.0F) return 0xFF9D32;
        return 0xFFE45C;
    }
}
