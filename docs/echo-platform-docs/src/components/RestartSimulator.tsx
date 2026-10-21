import { useState } from "preact/hooks";
import { restartRecovery, type RestartMoment } from "../lib/lifecycle";

const moments: RestartMoment[] = ["mid-enter", "mid-main", "stop-requested", "safe-exit", "mid-exit", "terminal"];

const labels: Record<RestartMoment, string> = {
  "mid-enter": "середина ENTER",
  "mid-main": "середина MAIN",
  "stop-requested": "остановка запрошена",
  "safe-exit": "безопасный EXIT",
  "mid-exit": "середина EXIT",
  terminal: "завершено",
};

export function RestartSimulator() {
  const [moment, setMoment] = useState<RestartMoment>("mid-main");
  const steps = restartRecovery(moment);
  return (
    <section class="interactive-panel recovery-laboratory" aria-labelledby="restart-title" data-spine-label="Лаборатория восстановления">
      <div class="stage-panel-copy">
        <p class="eyebrow">восстановление после перезапуска</p>
        <h3 id="restart-title">Лаборатория восстановления</h3>
        <p>Выберите момент перезапуска и проверьте, что ENTER не проигрывается повторно без причины.</p>
      </div>
      <div class="segmented" role="group" aria-label="Момент перезапуска">
        {moments.map((item) => (
          <button type="button" aria-pressed={moment === item} onClick={() => setMoment(item)}>
            {labels[item]}
          </button>
        ))}
      </div>
      <ol>
        {steps.map((step) => <li>{step}</li>)}
      </ol>
    </section>
  );
}
