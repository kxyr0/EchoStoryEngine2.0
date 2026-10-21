package io.github.kxyr0.echodialogue.forge.diagnostic;

import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticSubject;
import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import java.util.Map;

/**
 * Player-facing Russian formatting for dialogue diagnostics.
 */
public final class DialogueDiagnosticDisplay {
    private static final Map<String, String> MESSAGES = Map.ofEntries(
            Map.entry("EDLG-SYNTAX-ESL-PARSE-001", "Неожиданный токен в DSL."),
            Map.entry("EDLG-SYNTAX-ESL-PARSE-002", "Ожидался другой токен в DSL."),
            Map.entry("EDLG-SYNTAX-ESL-PARSE-003", "Пропущена точка с запятой ';'."),
            Map.entry("EDLG-SYNTAX-ESL-PARSE-004", "Не закрыта круглая скобка ')'."),
            Map.entry("EDLG-SYNTAX-ESL-PARSE-005", "Не закрыта квадратная скобка ']'."),
            Map.entry("EDLG-SYNTAX-ESL-PARSE-006", "Не закрыта фигурная скобка '}'."),
            Map.entry("EDLG-SYNTAX-ESL-PARSE-013", "Не найдено объявление класса DialogueResource."),
            Map.entry("EDLG-SEM-011", "Line ссылается на неизвестный alias. Проверь Alias(...) в Init()."),
            Map.entry("EDLG-FORGE-001", "DialogueId объявлен повторно. Удали дубль или поменяй id."),
            Map.entry("EDLG-FORGE-002", "Reload отклонён. Исправь ошибки выше; старый каталог оставлен."),
            Map.entry("EDLG-FORGE-004", "Не удалось прочитать .dialogue.es файл. Проверь UTF-8 и datapack."));

    public String reloadFailureHeader(int errorCount) {
        return "Echo Dialogue reload: ошибка, каталог не обновлён. Ошибок: " + errorCount;
    }

    public String hiddenErrors(int hiddenErrors) {
        return "- Ещё ошибок: " + hiddenErrors + ". Подробно: /echo_dialogue diagnostics 20";
    }

    public String error(DialogueDiagnostic diagnostic) {
        return "- " + location(diagnostic) + " | Ошибка " + diagnostic.code() + ": " + message(diagnostic);
    }

    public String command(DialogueDiagnostic diagnostic) {
        return severity(diagnostic) + " " + diagnostic.code() + " | "
                + location(diagnostic) + " | " + message(diagnostic);
    }

    public boolean isReloadSummary(DialogueDiagnostic diagnostic) {
        return "EDLG-FORGE-002".equals(diagnostic.code());
    }

    private String location(DialogueDiagnostic diagnostic) {
        return diagnostic.subject().map(this::location).orElse("Файл: не указан | Строка: не указана");
    }

    private String location(DiagnosticSubject subject) {
        if (!"source".equals(subject.kind())) {
            return "Объект: " + subject.kind() + " " + subject.value();
        }
        return "Файл: " + dataPath(subject.value())
                + " | Строка: " + subject.line().map(String::valueOf).orElse("не указана")
                + " | Колонка: " + subject.column().map(String::valueOf).orElse("не указана");
    }

    private String dataPath(String sourceId) {
        int separator = sourceId.indexOf(':');
        if (separator <= 0 || separator == sourceId.length() - 1) {
            return sourceId;
        }
        return "data/" + sourceId.substring(0, separator) + "/" + sourceId.substring(separator + 1);
    }

    private String message(DialogueDiagnostic diagnostic) {
        String message = MESSAGES.get(diagnostic.code());
        if (message != null) {
            return message;
        }
        if ("EDLG-SEM-UNSUPPORTED".equals(diagnostic.code())) {
            return unsupported(diagnostic.message());
        }
        return translatedFallback(diagnostic.message());
    }

    private String unsupported(String message) {
        String feature = afterColon(message);
        return "Профиль DialogueResource D1 не поддерживает: " + feature + ".";
    }

    private String afterColon(String message) {
        int separator = message.indexOf(':');
        if (separator < 0 || separator == message.length() - 1) {
            return message;
        }
        return message.substring(separator + 1).trim();
    }

    private String translatedFallback(String message) {
        if (message.contains("Missing semicolon")) {
            return "Пропущена точка с запятой ';'.";
        }
        if (message.contains("Missing class declaration")) {
            return "Не найдено объявление класса DialogueResource.";
        }
        if (message.contains("unknown intrinsic")) {
            return "Использована неизвестная DSL-функция.";
        }
        return "Ошибка DSL: " + message;
    }

    private String severity(DialogueDiagnostic diagnostic) {
        return switch (diagnostic.severity()) {
            case ERROR -> "ОШИБКА";
            case WARNING -> "ПРЕДУПРЕЖДЕНИЕ";
            case INFO -> "ИНФО";
        };
    }
}
