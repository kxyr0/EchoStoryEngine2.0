const proof = [
  ["вызовов parser", "1"],
  ["вызовов binder", "1"],
  ["delta revision при ensure без изменений", "0"],
  ["граница обработки", "64 записей/тик"],
  ["binding в production-smoke проверке", "1"],
];

export function ProofPanel() {
  return (
    <section class="interactive-panel proof-panel" aria-labelledby="proof-title" data-spine-label="Панель доказательств">
      <div class="stage-panel-copy">
        <p class="eyebrow">доказательство во время игры</p>
        <h3 id="proof-title">Панель доказательств</h3>
      </div>
      <div class="state-line">
        {proof.map(([label, value]) => (
          <div class="state-node active"><strong>{value}</strong><p>{label}</p></div>
        ))}
      </div>
    </section>
  );
}
