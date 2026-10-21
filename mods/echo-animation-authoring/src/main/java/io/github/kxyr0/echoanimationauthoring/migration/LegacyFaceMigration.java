package io.github.kxyr0.echoanimationauthoring.migration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic migration for archived legacy facial animation snippets.
 */
public final class LegacyFaceMigration {
    public MigrationResult migrate(String source, String animationId, String rigId) {
        List<String> warnings = new ArrayList<>();
        int durationMillis = millis(number(source, "lengthSeconds", 1.0D));
        String end = endBehavior(source, warnings);
        StringBuilder output = new StringBuilder();
        output.append("namespace JDH.Animation;\n\n");
        output.append("class MigratedFaceAnimation : FacialAnimationResource\n{\n");
        output.append("    public override FacialAnimationDef Build()\n    {\n");
        output.append("        return FacialAnimation(\"")
                .append(animationId)
                .append("\", rig: \"")
                .append(rigId)
                .append("\",\n");
        output.append("            durationMillis: ")
                .append(durationMillis)
                .append(", end: \"")
                .append(end)
                .append("\",\n");
        output.append("            tracks: [").append(tracks(source, durationMillis, warnings)).append("]);\n");
        output.append("    }\n}\n");
        unresolved(source, warnings);
        return new MigrationResult(output.toString(), warnings);
    }

    private String tracks(String source, int durationMillis, List<String> warnings) {
        List<String> tracks = new ArrayList<>();
        moveTrack(source, "left", "brows_left", durationMillis).ifPresent(tracks::add);
        moveTrack(source, "right", "brows_right", durationMillis).ifPresent(tracks::add);
        eyelidTrack(source, "upperEyelids", "upper_eyelids", durationMillis).ifPresent(tracks::add);
        eyelidTrack(source, "lowerEyelids", "lower_eyelids", durationMillis).ifPresent(tracks::add);
        if (source.contains("tilt")) {
            warnings.add("Поле tilt не мигрировано автоматически: старая ось вращения не доказана.");
        }
        return String.join(", ", tracks);
    }

    private java.util.Optional<String> moveTrack(String source, String side, String bone, int durationMillis) {
        Pattern pattern = Pattern.compile(side + "\\s*:\\s*\\{[\\s\\S]*?move\\s*:\\s*\\{\\s*x\\s*:\\s*"
                + "(-?\\d+(?:\\.\\d+)?)\\s*,\\s*y\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
        Matcher matcher = pattern.matcher(source);
        if (!matcher.find()) {
            return java.util.Optional.empty();
        }
        int x = millis(Double.parseDouble(matcher.group(1)));
        int y = millis(Double.parseDouble(matcher.group(2)));
        return java.util.Optional.of(track(bone, durationMillis, "x: " + x + ", y: " + y));
    }

    private java.util.Optional<String> eyelidTrack(String source, String field, String bone, int durationMillis) {
        Pattern pattern = Pattern.compile(field + "\\s*:\\s*\\{\\s*to\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
        Matcher matcher = pattern.matcher(source);
        if (!matcher.find()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(track(bone, durationMillis, "y: " + millis(Double.parseDouble(matcher.group(1)))));
    }

    private String track(String bone, int durationMillis, String endFields) {
        return "Track(\"" + bone + "\", frames: [Frame(0), Frame(" + durationMillis + ", " + endFields + ")])";
    }

    private double number(String source, String field, double fallback) {
        Matcher matcher = Pattern.compile(field + "\\s*:\\s*(-?\\d+(?:\\.\\d+)?)").matcher(source);
        return matcher.find() ? Double.parseDouble(matcher.group(1)) : fallback;
    }

    private int millis(double value) {
        return (int) Math.round(value * 1000.0D);
    }

    private String endBehavior(String source, List<String> warnings) {
        Matcher matcher = Pattern.compile("end\\s*:\\s*\"?([A-Za-z_]+)\"?").matcher(source);
        if (!matcher.find()) {
            warnings.add("Поле end отсутствует; применён HOLD.");
            return "HOLD";
        }
        String value = matcher.group(1).toLowerCase(Locale.ROOT);
        if (value.equals("hold")) {
            return "HOLD";
        }
        warnings.add("Поле end требует проверки автора: " + value);
        return "HOLD";
    }

    private void unresolved(String source, List<String> warnings) {
        if (source.contains("blinkTravelY")) {
            warnings.add("Поле blinkTravelY сохранено как предупреждение: нужна проверка character calibration.");
        }
    }
}
