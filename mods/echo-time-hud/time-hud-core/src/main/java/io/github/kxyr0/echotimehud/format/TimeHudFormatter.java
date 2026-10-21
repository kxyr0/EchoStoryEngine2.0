package io.github.kxyr0.echotimehud.format;

import io.github.kxyr0.echotimehud.api.HudConfig;
import java.util.Locale;

public final class TimeHudFormatter {
    public String format(long day, int hour, int minute, HudConfig config) {
        String time = String.format(Locale.ROOT, "%02d:%02d", hour, minute);
        return config.showDay() ? "Day " + day + " · " + time : time;
    }
}

