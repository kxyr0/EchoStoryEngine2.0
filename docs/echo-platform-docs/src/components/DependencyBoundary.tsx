import { useState } from "preact/hooks";

type EdgeId = "npc-adapter" | "adapter-animation" | "animation-npc";

const modules = [
  { id: "story", label: "История", x: 12, y: 30 },
  { id: "dialogue", label: "Диалоги", x: 15, y: 68 },
  { id: "npc", label: "Echo NPC", x: 36, y: 50 },
  { id: "adapter", label: "Адаптер", x: 58, y: 50 },
  { id: "animation", label: "Echo Animation", x: 82, y: 50 },
  { id: "content", label: "Контент", x: 58, y: 18 },
];

const edges: Record<EdgeId, {
  label: string;
  allowed: boolean;
  from: string;
  to: string;
  why: string;
  imports: string[];
  contracts: string[];
  owner: string;
}> = {
  "npc-adapter": {
    label: "NPC -> Адаптер",
    allowed: true,
    from: "npc",
    to: "adapter",
    why: "Адаптер читает публичные снимки NPC и не требует приватного состояния сущности.",
    imports: ["io.github.kxyr.echoNPC.forge.api.NpcService", "io.github.kxyr.echoNPC.forge.api.ActorId"],
    contracts: ["ActorId", "NpcDefinitionId", "NpcMaterializationSnapshot"],
    owner: "echo-npc-animation-adapter",
  },
  "adapter-animation": {
    label: "Адаптер -> Анимация",
    allowed: true,
    from: "adapter",
    to: "animation",
    why: "Адаптер создаёт subject и запускает playback через публичный Animation API.",
    imports: ["io.github.kxyr.echoAnimation.api.AnimationService", "io.github.kxyr.echoAnimation.api.AnimationSubjectId"],
    contracts: ["AnimationProfileId", "AnimationSubjectId", "AnimationPlaybackId"],
    owner: "echo-npc-animation-adapter",
  },
  "animation-npc": {
    label: "Анимация -> NPC",
    allowed: false,
    from: "animation",
    to: "npc",
    why: "Ядро Animation остаётся нейтральным к NPC. Обратный import создаёт жесткую зависимость и ломает границу модулей.",
    imports: ["io.github.kxyr.echoNPC.forge.internal.entity.EchoNpcEntity"],
    contracts: ["запрещено: используйте границу адаптера"],
    owner: "код должен жить в адаптере, не в echo-animation",
  },
};

function point(id: string) {
  return modules.find((module) => module.id === id) ?? modules[0];
}

export function DependencyField() {
  const [edge, setEdge] = useState<EdgeId>("npc-adapter");
  const selected = edges[edge];
  const from = point(selected.from);
  const to = point(selected.to);

  return (
    <section class="interactive-panel dependency-field" aria-labelledby="dependency-field-title" data-spine-label="Поле зависимостей">
      <div class="stage-panel-copy">
        <p class="eyebrow">граница модулей</p>
        <h3 id="dependency-field-title">Поле зависимостей</h3>
        <p>Выберите связь и проверьте, проходит ли она через публичный контракт или упирается в границу модуля.</p>
      </div>

      <div class="dependency-canvas">
        <svg viewBox="0 0 100 86" aria-hidden="true">
          <path class="dependency-line muted" d="M12 30 C30 18, 42 18, 58 18" />
          <path class="dependency-line muted" d="M15 68 C28 72, 42 66, 58 50" />
          <path
            class={`dependency-line ${selected.allowed ? "allowed" : "blocked"}`}
            d={`M${from.x} ${from.y} C${(from.x + to.x) / 2} ${from.y - 18}, ${(from.x + to.x) / 2} ${to.y + 18}, ${to.x} ${to.y}`}
          />
          {!selected.allowed && <path class="dependency-boundary" d="M61 25 V75" />}
        </svg>
        {modules.map((module) => (
          <button
            type="button"
            class={`dependency-node node-${module.id}`}
            style={{ left: `${module.x}%`, top: `${module.y}%` }}
            data-active={module.id === selected.from || module.id === selected.to}
            data-lens-subject={module.label}
          >
            {module.label}
          </button>
        ))}
      </div>

      <div class="dependency-controls" role="group" aria-label="Проверяемая связь">
        {(Object.keys(edges) as EdgeId[]).map((id) => (
          <button type="button" aria-pressed={edge === id} onClick={() => setEdge(id)}>
            {edges[id].label}
          </button>
        ))}
      </div>

      <div class={`boundary-readout ${selected.allowed ? "is-allowed" : "is-blocked"}`}>
        <strong>{selected.allowed ? "Связь разрешена" : "Связь запрещена"}</strong>
        <p>{selected.why}</p>
        <dl>
          <div>
            <dt>Публичные контракты</dt>
            <dd>{selected.contracts.map((contract) => <code>{contract}</code>)}</dd>
          </div>
          <div>
            <dt>Java imports</dt>
            <dd>{selected.imports.map((contract) => <code>{contract}</code>)}</dd>
          </div>
          <div>
            <dt>Где должен жить код</dt>
            <dd>{selected.owner}</dd>
          </div>
        </dl>
      </div>
    </section>
  );
}

export const DependencyBoundary = DependencyField;
