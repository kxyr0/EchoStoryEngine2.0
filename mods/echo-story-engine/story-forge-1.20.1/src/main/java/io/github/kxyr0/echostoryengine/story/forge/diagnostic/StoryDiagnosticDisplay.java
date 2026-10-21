package io.github.kxyr0.echostoryengine.story.forge.diagnostic;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import java.util.Map;
import java.util.Optional;

/**
 * Player-facing Russian formatting for story and environment diagnostics.
 */
public final class StoryDiagnosticDisplay {
    private static final Map<String, String> SYNTAX_MESSAGES = Map.ofEntries(
            Map.entry("ESL-PARSE-001", "Неожиданный токен в DSL."),
            Map.entry("ESL-PARSE-002", "Ожидался другой токен в DSL."),
            Map.entry("ESL-PARSE-003", "Пропущена точка с запятой ';'."),
            Map.entry("ESL-PARSE-004", "Не закрыта круглая скобка ')'."),
            Map.entry("ESL-PARSE-005", "Не закрыта квадратная скобка ']'."),
            Map.entry("ESL-PARSE-006", "Не закрыта фигурная скобка '}'."),
            Map.entry("ESL-PARSE-013", "Не найдено объявление класса StoryResource."));
    private static final Map<String, String> STATIC_MESSAGES = Map.ofEntries(
            Map.entry("ESE-FORGE-RESOURCE-002",
                    "Reload отклонён. Исправь ошибки выше; старый каталог оставлен."),
            Map.entry("ESE-FORGE-RESOURCE-003",
                    "StoryId объявлен повторно. Оставь один источник или поменяй id."),
            Map.entry("ESE-FORGE-RESOURCE-004", "Не удалось прочитать .es файл. Проверь UTF-8 и datapack."),
            Map.entry("ESE-FORGE-RESOURCE-005", "В одной папке слишком много .es файлов."));

    public String reloadFailureHeader(int errorCount) {
        return "Echo Story reload: ошибка, каталог не обновлён. Ошибок: " + errorCount;
    }

    public String hiddenErrors(int hiddenErrors) {
        return "- Ещё ошибок: " + hiddenErrors + ". Подробно: /echo_story diagnostics 20";
    }

    public String error(StoryDiagnostic diagnostic) {
        return "- " + location(diagnostic) + " | Ошибка " + diagnostic.code() + ": " + message(diagnostic);
    }

    public String command(StoryDiagnostic diagnostic) {
        return severity(diagnostic) + " " + diagnostic.code() + " | "
                + location(diagnostic) + " | " + message(diagnostic)
                + " | Причина: " + diagnostic.causeCategory();
    }

    public boolean isReloadSummary(StoryDiagnostic diagnostic) {
        return "ESE-FORGE-RESOURCE-002".equals(diagnostic.code());
    }

    private String location(StoryDiagnostic diagnostic) {
        Optional<String> sourceId = sourceId(diagnostic);
        return "Файл: " + sourceId.map(this::dataPath).orElse("не указан")
                + " | Строка: " + value(diagnostic, "line").orElse("не указана")
                + " | Колонка: " + value(diagnostic, "column").orElse("не указана");
    }

    private Optional<String> sourceId(StoryDiagnostic diagnostic) {
        Optional<String> fromContext = value(diagnostic, "sourceId");
        if (fromContext.isPresent()) {
            return fromContext;
        }
        String sourceLocation = diagnostic.subject().sourceLocation();
        if (looksLikeResourceId(sourceLocation)) {
            return Optional.of(sourceLocation);
        }
        String correlationId = diagnostic.subject().correlationId();
        return looksLikeResourceId(correlationId) ? Optional.of(correlationId) : Optional.empty();
    }

    private Optional<String> value(StoryDiagnostic diagnostic, String key) {
        String value = diagnostic.context().getOrDefault(key, "");
        return value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    private boolean looksLikeResourceId(String value) {
        int separator = value.indexOf(':');
        return separator > 0 && separator < value.length() - 1 && value.contains(".es");
    }

    private String dataPath(String sourceId) {
        int separator = sourceId.indexOf(':');
        if (separator <= 0 || separator == sourceId.length() - 1) {
            return sourceId;
        }
        return "data/" + sourceId.substring(0, separator) + "/" + sourceId.substring(separator + 1);
    }

    private String message(StoryDiagnostic diagnostic) {
        String message = STATIC_MESSAGES.get(diagnostic.code());
        if (message != null) {
            return message;
        }
        return switch (diagnostic.code()) {
            case "ESE-SOURCE-001" -> syntaxMessage(diagnostic);
            case "ESE-SOURCE-014" -> resourceKind(diagnostic) + " использует неподдерживаемый профиль.";
            case "ESE-SOURCE-015" -> "Не найден класс " + resourceKind(diagnostic) + ".";
            case "ESE-SOURCE-021" -> unknownIntrinsic(diagnostic);
            default -> translatedFallback(diagnostic.message());
        };
    }

    private String syntaxMessage(StoryDiagnostic diagnostic) {
        String message = SYNTAX_MESSAGES.get(diagnostic.context().getOrDefault("languageCode", ""));
        return message == null ? translatedFallback(diagnostic.message()) : message;
    }

    private String unknownIntrinsic(StoryDiagnostic diagnostic) {
        String intrinsic = diagnostic.context().getOrDefault("intrinsic", "");
        if (intrinsic.isBlank()) {
            return resourceKind(diagnostic) + " использует неизвестную DSL-функцию.";
        }
        return resourceKind(diagnostic) + " использует неизвестную DSL-функцию: " + intrinsic + ".";
    }

    private String resourceKind(StoryDiagnostic diagnostic) {
        return sourceId(diagnostic).orElse("").contains("/environments/") ? "EnvironmentResource" : "StoryResource";
    }

    private String translatedFallback(String message) {
        if (message.contains("Missing class declaration")) {
            return "Не найдено объявление класса ресурса.";
        }
        if (message.contains("Missing semicolon")) {
            return "Пропущена точка с запятой ';'.";
        }
        if (message.contains("unknown intrinsic")) {
            return "Использована неизвестная DSL-функция.";
        }
        return "Ошибка DSL: " + message;
    }

    private String severity(StoryDiagnostic diagnostic) {
        return switch (diagnostic.severity()) {
            case ERROR -> "ОШИБКА";
            case WARN -> "ПРЕДУПРЕЖДЕНИЕ";
            case INFO -> "ИНФО";
            default -> diagnostic.severity().name();
        };
    }
}
