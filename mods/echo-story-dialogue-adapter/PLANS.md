# Планы

Execution plan обязателен для D3, потому что milestone создаёт новый репозиторий, меняет multi-repository dependency
graph и требует публичных API boundaries.

Планы живут в `docs/plans`.

Каждый план должен содержать:

- цель;
- non-goals;
- результаты аудита;
- точные публичные Story types;
- точные публичные Dialogue types;
- public API gaps;
- planned API additions;
- dependency/publication strategy;
- start/recovery/cancellation/terminal flows;
- tests and acceptance;
- risks;
- rollback;
- verification.

Не переписывать выполненный план так, будто discoveries не было. Добавлять уточнения и deviations отдельными секциями.

