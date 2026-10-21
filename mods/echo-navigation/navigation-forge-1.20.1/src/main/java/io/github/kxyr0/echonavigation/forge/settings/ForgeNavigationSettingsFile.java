package io.github.kxyr0.echonavigation.forge.settings;

import io.github.kxyr0.echonavigation.api.backend.NavigationSettingsSnapshot;
import io.github.kxyr0.echonavigation.api.diagnostic.NavigationDiagnostic;
import io.github.kxyr0.echonavigation.api.identity.NavigationBackendId;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class ForgeNavigationSettingsFile {
    private static final String NAME = "echo-navigation-server.toml";
    private final Path path;

    ForgeNavigationSettingsFile(Path gameDir) {
        this.path = gameDir.resolve("config").resolve(NAME);
    }

    Result load(long nextRevision) {
        try {
            ensureExists();
            Map<String, String> values = parse(Files.readAllLines(path));
            return new Result(Optional.of(snapshot(values, nextRevision)), List.of());
        } catch (RuntimeException | IOException exception) {
            return new Result(Optional.empty(), List.of(NavigationDiagnostic.error(
                    "NAVIGATION_BACKEND_SETTINGS_INVALID", exception.getMessage(), Map.of("file", path.toString()))));
        }
    }

    void write(NavigationSettingsSnapshot settings) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, text(settings));
    }

    private void ensureExists() throws IOException {
        if (!Files.isRegularFile(path)) {
            write(NavigationSettingsSnapshot.defaults());
        }
    }

    private Map<String, String> parse(List<String> lines) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String line : lines) {
            String cleaned = line.split("#", 2)[0].trim();
            if (cleaned.isEmpty()) {
                continue;
            }
            String[] parts = cleaned.split("=", 2);
            if (parts.length == 2) {
                values.put(parts[0].trim(), parts[1].trim().replace("\"", ""));
            }
        }
        return values;
    }

    private NavigationSettingsSnapshot snapshot(Map<String, String> values, long revision) {
        return new NavigationSettingsSnapshot(revision, new NavigationBackendId(value(values, "backend")),
                doubleValue(values, "arrival_tolerance"), intValue(values, "repath_interval_ticks"),
                intValue(values, "max_replans"), intValue(values, "progress_sample_interval_ticks"),
                doubleValue(values, "minimum_progress_distance"), intValue(values, "stuck_timeout_ticks"),
                intValue(values, "max_move_duration_ticks"));
    }

    private String value(Map<String, String> values, String key) {
        return Optional.ofNullable(values.get(key))
                .orElseGet(() -> parse(text(NavigationSettingsSnapshot.defaults()).lines().toList()).get(key));
    }

    private int intValue(Map<String, String> values, String key) {
        return Integer.parseInt(value(values, key));
    }

    private double doubleValue(Map<String, String> values, String key) {
        return Double.parseDouble(value(values, key));
    }

    private String text(NavigationSettingsSnapshot settings) {
        return "backend = \"" + settings.backendId().value() + "\"\n"
                + "arrival_tolerance = " + settings.arrivalTolerance() + "\n"
                + "repath_interval_ticks = " + settings.repathIntervalTicks() + "\n"
                + "max_replans = " + settings.maxReplans() + "\n"
                + "progress_sample_interval_ticks = " + settings.progressSampleIntervalTicks() + "\n"
                + "minimum_progress_distance = " + settings.minimumProgressDistance() + "\n"
                + "stuck_timeout_ticks = " + settings.stuckTimeoutTicks() + "\n"
                + "max_move_duration_ticks = " + settings.maxMoveDurationTicks() + "\n";
    }

    record Result(Optional<NavigationSettingsSnapshot> settings, List<NavigationDiagnostic> diagnostics) {
    }
}
