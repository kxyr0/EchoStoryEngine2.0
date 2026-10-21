const failures = [
  ["NPC без binding", "Оставить обычный renderer NPC", "EANIM-A4-UNBOUND", "Не создавать скрытый запасной subject"],
  ["Профиль не найден", "Отклонить provisioning", "EANIM-A4-PROFILE-NOT-FOUND", "Не сбрасывать существующий playback"],
  ["Конфликт provenance", "Оставить прежний subject", "EANIM-A4-PROVENANCE", "Не забирать subject у ручного владельца"],
  ["Снимок обрезан", "Пропустить orphan retirement", "EANIM-A4-BOUNDED", "Не удалять неизвестных актёров"],
];

export function FailureMatrix() {
  return (
    <section class="interactive-panel contract-grid diagnostic-journal" aria-labelledby="failure-title" data-spine-label="Журнал отказов">
      <div class="stage-panel-copy">
        <p class="eyebrow">диагностика отказов</p>
        <h3 id="failure-title">Журнал отказов A4</h3>
        <p>Каждый отказ показывает допустимое восстановление и действие, которое система во время игры не имеет права выполнять.</p>
      </div>
      <div class="matrix-scroll">
        <table>
          <caption>Ожидаемое поведение при ошибках жизненного цикла адаптера</caption>
          <thead><tr><th>Ошибка</th><th>Восстановление</th><th>Код</th><th>Запрещённое действие</th></tr></thead>
          <tbody>
            {failures.map(([error, fallback, diagnostic, forbidden]) => (
              <tr><td>{error}</td><td>{fallback}</td><td><code>{diagnostic}</code></td><td>{forbidden}</td></tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
