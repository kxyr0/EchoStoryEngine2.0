package io.github.kxyr0.echoanimation.echoscript.diagnostic;

import io.github.kxyr0.echoanimation.api.diagnostic.AnimationDiagnostic;
import io.github.kxyr0.echoscript.api.source.SourceSpan;
import java.util.Map;
import java.util.Objects;

/**
 * Russian, source-aware diagnostic text for animation authoring.
 */
public final class AnimationDiagnosticMessages {
    private static final Map<String, String> EXACT = Map.ofEntries(
            Map.entry("Resource must contain exactly one Build method.",
                    "Ресурс должен содержать ровно один метод Build."),
            Map.entry("Build returns an intrinsic that does not match the resource base type.",
                    "Build возвращает intrinsic, который не соответствует базовому типу ресурса."),
            Map.entry("No value present", "Не найден обязательный аргумент."),
            Map.entry("Exactly one body rig is required for animations.",
                    "Для .animation.es нужен ровно один body rig в каталоге."),
            Map.entry("AnimationProfile must declare at least one initial state.",
                    "AnimationProfile должен объявить хотя бы один initial state."),
            Map.entry("Entry variants must exactly match allowed source states.",
                    "Entry variants должны точно совпадать с разрешенными source states."),
            Map.entry("Loop animation requires at least one exit point.",
                    "Loop-анимация должна иметь хотя бы одну exit point."),
            Map.entry("Every exit point requires exactly one exit variant.",
                    "Для каждой exit point нужен ровно один exit variant."),
            Map.entry("MISSING_ENTRY_BONE_COVERAGE", "Entry clip не покрывает все нужные bones."),
            Map.entry("MISSING_EXIT_BONE_COVERAGE", "Exit clip не покрывает все нужные bones."),
            Map.entry("Use either durationTicks or durationMillis, not both.",
                    "Используй либо durationTicks, либо durationMillis, но не оба сразу."),
            Map.entry("Face Frame Rotation supports only Z; use Rotation(0, 0, z).",
                    "Face Frame Rotation поддерживает только Z; используй Rotation(0, 0, z).")
    );
    private static final Map<String, String> PREFIX = Map.ofEntries(
            Map.entry("Unsupported field: ", "Неподдерживаемое поле: "),
            Map.entry("Unsupported Frame transform field: ", "Неподдерживаемое поле Frame: "),
            Map.entry("Unsupported Frame transform modifier: ", "Неподдерживаемый модификатор Frame: "),
            Map.entry("Expected intrinsic ", "Ожидался intrinsic "),
            Map.entry("Duplicate profile initial state channel: ",
                    "Повторяющийся initial state channel в профиле: "),
            Map.entry("Duplicate body rig id: ", "Повторяющийся id body rig: "),
            Map.entry("Clip references missing body rig: ", "Клип ссылается на отсутствующий body rig: "),
            Map.entry("Clip rig mismatch: ", "Rig клипа не совпадает: "),
            Map.entry("Unknown bone track: ", "Неизвестный bone track: "),
            Map.entry("Duplicate bone track: ", "Повторяющийся bone track: "),
            Map.entry("Duplicate AnimationId: ", "Повторяющийся AnimationId: "),
            Map.entry("Duplicate animation profile id: ", "Повторяющийся id animation profile: "),
            Map.entry("Profile references missing body rig: ", "Профиль ссылается на отсутствующий body rig: "),
            Map.entry("Unknown animation clip: ", "Неизвестный animation clip: "),
            Map.entry("Duplicate exit point: ", "Повторяющаяся exit point: "),
            Map.entry("Exit variant references missing exit point: ",
                    "Exit variant ссылается на отсутствующую exit point: "),
            Map.entry("No enum constant ", "Неизвестное enum-значение: ")
    );

    private AnimationDiagnosticMessages() {
    }

    public static AnimationDiagnostic source(AnimationDiagnostic diagnostic, SourceSpan span) {
        return source(diagnostic, span.sourceId().value(), span.start().line(), span.start().column());
    }

    public static AnimationDiagnostic source(AnimationDiagnostic diagnostic, String sourceId, String sourceText) {
        return source(diagnostic, sourceId, guessedLine(diagnostic.message(), sourceText), 1);
    }

    public static AnimationDiagnostic localized(AnimationDiagnostic diagnostic) {
        if (hasSource(diagnostic.message())) {
            return diagnostic;
        }
        return new AnimationDiagnostic(diagnostic.code(), diagnostic.severity(), translate(diagnostic.message()));
    }

    public static boolean hasSource(String message) {
        return message != null && message.startsWith("Файл: ");
    }

    public static String translate(String message) {
        String value = Objects.toString(message, "").trim();
        if (value.isEmpty()) {
            return "Неизвестная ошибка.";
        }
        String field = fieldMessage(value);
        if (field != null) {
            return field;
        }
        String exact = EXACT.get(value);
        if (exact != null) {
            return exact;
        }
        for (Map.Entry<String, String> entry : EXACT.entrySet()) {
            if (value.contains(entry.getKey())) {
                return value.replace(entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<String, String> entry : PREFIX.entrySet()) {
            if (value.contains(entry.getKey())) {
                return value.replace(entry.getKey(), entry.getValue());
            }
        }
        return value.startsWith("Ошибка") || value.startsWith("Ожидался") ? value : "Ошибка анимации: " + value;
    }

    public static String sourceNeedle(String message) {
        String value = Objects.toString(message, "");
        for (String prefix : PREFIX.keySet()) {
            String token = tokenAfter(value, prefix);
            if (!token.isEmpty()) {
                return token;
            }
        }
        String id = leadingResourceId(value);
        if (!id.isEmpty()) {
            return id;
        }
        return quotedToken(value);
    }

    private static AnimationDiagnostic source(
            AnimationDiagnostic diagnostic,
            String sourceId,
            int line,
            int column
    ) {
        if (hasSource(diagnostic.message())) {
            return diagnostic;
        }
        String prefix = "Файл: " + sourcePath(sourceId) + " | Строка: " + line + " | Столбец: " + column + " | ";
        String message = prefix + translate(diagnostic.message());
        return new AnimationDiagnostic(diagnostic.code(), diagnostic.severity(), message);
    }

    private static int guessedLine(String message, String sourceText) {
        String needle = sourceNeedle(message);
        if (needle.isEmpty()) {
            return 1;
        }
        String[] lines = Objects.toString(sourceText, "").split("\\R", -1);
        for (int index = 0; index < lines.length; index++) {
            if (lines[index].contains(needle)) {
                return index + 1;
            }
        }
        return 1;
    }

    private static String fieldMessage(String value) {
        if (!value.startsWith("Field ") && !value.startsWith("Positional argument ")) {
            return null;
        }
        return value.replace("Field ", "Поле ")
                .replace("Positional argument", "Позиционный аргумент")
                .replace(" must be integer-milli or decimal string.", " должно быть integer-milli или decimal string.")
                .replace(" must be decimal string: ", " должно быть decimal string: ")
                .replace(" must be finite.", " должно быть конечным числом.")
                .replace(" must not be blank.", " не должно быть пустым.");
    }

    private static String sourcePath(String sourceId) {
        int separator = sourceId.indexOf(':');
        if (separator <= 0 || separator == sourceId.length() - 1) {
            return sourceId;
        }
        return "data/" + sourceId.substring(0, separator) + "/" + sourceId.substring(separator + 1);
    }

    private static String tokenAfter(String value, String prefix) {
        int index = value.indexOf(prefix);
        if (index < 0) {
            return "";
        }
        String tail = value.substring(index + prefix.length()).trim();
        int end = tail.indexOf(' ');
        String token = end < 0 ? tail : tail.substring(0, end);
        return token.replace(".", "").replace(",", "");
    }

    private static String leadingResourceId(String value) {
        int marker = value.indexOf(": ");
        if (marker <= 0) {
            return "";
        }
        String candidate = value.substring(0, marker);
        return candidate.indexOf(':') > 0 ? candidate : "";
    }

    private static String quotedToken(String value) {
        int first = value.indexOf('"');
        int second = first < 0 ? -1 : value.indexOf('"', first + 1);
        return second > first ? value.substring(first + 1, second) : "";
    }
}
