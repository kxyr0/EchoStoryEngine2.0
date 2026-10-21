import { useEffect, useState } from "preact/hooks";
import { type AnimationPhase, nextPhase } from "../lib/lifecycle";

const phases: Array<{
  id: AnimationPhase;
  label: string;
  clip: string;
  pose: string;
  lease: string;
}> = [
  { id: "ENTER", label: "Вход", clip: "jdh:raise_hand_enter", pose: "рука поднимается один раз", lease: "body: захвачен" },
  { id: "MAIN", label: "Основной цикл", clip: "jdh:wave_main", pose: "рука движется влево и вправо", lease: "body: удерживается" },
  { id: "EXIT", label: "Выход", clip: "jdh:lower_hand_exit", pose: "рука возвращается в idle", lease: "body: освобождается после кадра" },
  { id: "TARGET", label: "Цель", clip: "idle", pose: "стабильная поза", lease: "body: свободен" },
];

function phaseIndex(phase: AnimationPhase) {
  return phases.findIndex((item) => item.id === phase);
}

export function LifecycleStage() {
  const [state, setState] = useState({ phase: "ENTER" as AnimationPhase, cursorMs: 0, stopRequested: false, persistedRevision: 14 });
  const [playing, setPlaying] = useState(false);
  const active = phases[phaseIndex(state.phase)] ?? phases[0];

  useEffect(() => {
    if (!playing) {
      return;
    }
    const timer = window.setInterval(() => {
      setState((current) => nextPhase(current));
    }, 900);
    return () => window.clearInterval(timer);
  }, [playing]);

  useEffect(() => {
    if (state.phase === "TARGET") {
      setPlaying(false);
    }
  }, [state.phase]);

  return (
    <section class="interactive-panel lifecycle-stage" aria-labelledby="lifecycle-stage-title" data-spine-label="Стадии анимации">
      <div class="stage-panel-copy">
        <p class="eyebrow">жизненный цикл анимации</p>
        <h3 id="lifecycle-stage-title">Сцена ENTER-MAIN-EXIT</h3>
        <p>
          MAIN не должен каждый раз заново поднимать руку. ENTER выполняет подготовку, MAIN держит цикл,
          EXIT отрабатывает только после запроса остановки.
        </p>
      </div>

      <div class="lifecycle-track" aria-label="Фазы проигрывания">
        {phases.map((phase, index) => (
          <button
            type="button"
            class="phase-node"
            aria-pressed={state.phase === phase.id}
            style={{ "--phase-index": String(index) }}
            onClick={() => setState((current) => ({ ...current, phase: phase.id, cursorMs: 0 }))}
          >
            <span>{phase.id}</span>
            <strong>{phase.label}</strong>
          </button>
        ))}
        <span class="phase-connector" style={{ "--active-phase": String(Math.max(0, phaseIndex(state.phase))) }} aria-hidden="true" />
      </div>

      <div class="lifecycle-readout">
        <div class="pose-frame" data-phase={state.phase}>
          <span class="pose-head" />
          <span class="pose-body" />
          <span class="pose-arm pose-left" />
          <span class="pose-arm pose-right" />
          <span class="pose-leg pose-left" />
          <span class="pose-leg pose-right" />
        </div>
        <dl>
          <div>
            <dt>Текущий clip</dt>
            <dd><code>{active.clip}</code></dd>
          </div>
          <div>
            <dt>Поза</dt>
            <dd>{active.pose}</dd>
          </div>
          <div>
            <dt>Сохранённое состояние</dt>
            <dd><code>{state.phase}:{state.cursorMs}ms:r{state.persistedRevision}</code></dd>
          </div>
          <div>
            <dt>Захват канала</dt>
            <dd>{active.lease}</dd>
          </div>
          <div>
            <dt>Запрос остановки</dt>
            <dd>{state.stopRequested ? "ожидает безопасного выхода" : "не запрошен"}</dd>
          </div>
        </dl>
      </div>

      <div class="control-row" role="group" aria-label="Управление жизненным циклом">
          <button type="button" onClick={() => setPlaying((value) => !value)}>{playing ? "Пауза" : "Запустить"}</button>
        <button type="button" onClick={() => setState((current) => nextPhase(current))}>Шаг</button>
        <button type="button" onClick={() => setState((current) => ({ ...current, stopRequested: true }))}>Запрос остановки</button>
        <button type="button" onClick={() => setState({ phase: "ENTER", cursorMs: 0, stopRequested: false, persistedRevision: 14 })}>Сброс</button>
      </div>
    </section>
  );
}

export const AnimationLifecycleLab = LifecycleStage;
