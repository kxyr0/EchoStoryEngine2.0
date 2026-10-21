from __future__ import annotations

from enum import Enum


class Severity(Enum):
    OK = 0
    INFO = 1
    WARNING = 2
    CRITICAL = 3


class ModLoader(Enum):
    FABRIC = "Fabric"
    FORGE = "Forge"
    UNKNOWN = "Неизвестен"


class ModStatus(Enum):
    OK = ("Актуален", Severity.OK, "check", False)
    OUTDATED = ("Требуется новая версия", Severity.WARNING, "warning", True)
    NEWER = ("Новее требуемой", Severity.INFO, "warning", False)
    MISSING_REQUIRED = ("Обязательный мод отсутствует", Severity.CRITICAL, "error", True)
    MISSING_OPTIONAL = ("Необязательный мод отсутствует", Severity.INFO, "warning", False)
    DUPLICATE = ("Дубликат", Severity.WARNING, "duplicate", True)
    BAD_JAR = ("JAR повреждён", Severity.CRITICAL, "error", True)
    BAD_METADATA = ("Ошибка метаданных", Severity.CRITICAL, "error", True)
    UNRECOGNIZED = ("Формат не распознан", Severity.CRITICAL, "error", True)
    UNKNOWN_ECHO = ("Нет в manifest.json", Severity.INFO, "warning", False)
    INVALID_VERSION = ("Некорректная версия", Severity.CRITICAL, "error", True)

    def __init__(self, label: str, severity: Severity, icon_name: str, is_problem: bool) -> None:
        self.label = label
        self.severity = severity
        self.icon_name = icon_name
        self.is_problem = is_problem
