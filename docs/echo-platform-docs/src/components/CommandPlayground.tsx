import { useState } from "preact/hooks";

export function CommandPlayground() {
  const [animation, setAnimation] = useState("jdh:wave");
  const [npc, setNpc] = useState("jdh:john_father");
  const [occupied, setOccupied] = useState(false);
  const result = occupied ? "Отказ: channel lease занят" : "Старт: ActorId найден, binding проверен";
  return (
    <section class="interactive-panel command-playground" aria-labelledby="command-title" data-spine-label="Проверка команды">
      <div class="stage-panel-copy">
        <p class="eyebrow">командная проверка</p>
        <h3 id="command-title">Проверка play/stop</h3>
      </div>
      <label>AnimationId <input value={animation} onInput={(event) => setAnimation(event.currentTarget.value)} /></label>
      <label>ActorId <input value={npc} onInput={(event) => setNpc(event.currentTarget.value)} /></label>
      <label><input type="checkbox" checked={occupied} onInput={(event) => setOccupied(event.currentTarget.checked)} /> канал занят</label>
      <p><code>/echo_animation play_animation {animation} {npc}</code></p>
      <p><code>/echo_animation stop_animation {npc} {animation}</code></p>
      <p>{result}</p>
    </section>
  );
}
