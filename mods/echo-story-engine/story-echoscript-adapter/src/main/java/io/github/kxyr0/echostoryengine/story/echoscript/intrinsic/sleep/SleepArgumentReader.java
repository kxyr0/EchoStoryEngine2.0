package io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.sleep;

import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.IntrinsicCallReader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class SleepArgumentReader {
    private static final Set<String> CONTEXT = Set.of("player", "scenario", "owner", "command");
    private final SleepDreamTargetArguments dreamTargets = new SleepDreamTargetArguments();

    Map<String, String> read(IntrinsicCallReader reader, String intrinsic, int offset,
                             Map<String, String> blackspaceVariables) {
        Map<String, String> arguments = new LinkedHashMap<>();
        switch (intrinsic) {
            case "Sleep.Arm" -> scenario(arguments, reader, intrinsic, offset);
            case "Sleep.CloseEyes" -> duration(arguments, reader, intrinsic, offset);
            case "Sleep.EnterDream" -> arguments.putAll(dreamTargets.read(
                    reader, intrinsic, offset, blackspaceVariables));
            case "Sleep.OpenEyes" -> openEyes(arguments, reader, intrinsic, offset);
            case "Sleep.Wait", "Sleep.WaitSeconds" -> waitFor(arguments, reader, intrinsic, offset);
            case "Sleep.ReturnToBed" -> put(arguments, "mode", reader.optionalNamedScalar("mode", intrinsic));
            default -> { }
        }
        CONTEXT.stream().filter(name -> !arguments.containsKey(name)).forEach(name ->
                put(arguments, name, reader.optionalNamedScalar(name, intrinsic)));
        return arguments;
    }

    private void scenario(Map<String, String> values, IntrinsicCallReader reader,
                          String intrinsic, int index) {
        put(values, "scenario", reader.positionalCount() > index
                ? reader.positionalScalar(index, intrinsic, "scenario")
                : reader.requiredNamedScalar("scenario", intrinsic));
    }

    private void duration(Map<String, String> values, IntrinsicCallReader reader,
                          String intrinsic, int index) {
        put(values, "duration", reader.positionalCount() > index
                ? reader.positionalScalar(index, intrinsic, "duration")
                : reader.optionalNamedScalar("duration", intrinsic));
    }

    private void openEyes(Map<String, String> values, IntrinsicCallReader reader,
                          String intrinsic, int offset) {
        duration(values, reader, intrinsic, offset);
        put(values, "sleepyBlink", reader.optionalNamedScalar("sleepyBlink", intrinsic));
        put(values, "showHud", reader.optionalNamedScalar("showHud", intrinsic));
    }

    private void waitFor(Map<String, String> values, IntrinsicCallReader reader,
                         String intrinsic, int offset) {
        if (reader.positionalCount() > offset) {
            put(values, "seconds", reader.positionalScalar(offset, intrinsic, "seconds"));
        } else {
            put(values, "seconds", reader.optionalNamedScalar("seconds", intrinsic));
            put(values, "duration", reader.optionalNamedScalar("duration", intrinsic));
            put(values, "ticks", reader.optionalNamedScalar("ticks", intrinsic));
        }
    }

    private void put(Map<String, String> values, String key, Optional<String> value) {
        value.ifPresent(present -> values.put(key, present));
    }
}

