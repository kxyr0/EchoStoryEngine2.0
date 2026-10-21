# ADR 0005: One Active Session Per Player

Status: accepted for D1.

D1 policy: вторая active session для той же audience отклоняется.

Reason:

- это явно и безопасно;
- session overwrite скрывал бы ошибку authoring или command usage;
- replace/cancel policy можно добавить позже как отдельное решение.
