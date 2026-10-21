import { useState } from "preact/hooks";

const nodes = [
  { id: "story", title: "История", note: "Событие сюжета", href: "/docs/adapters/story-npc/", x: 18, y: 34 },
  { id: "dialogue", title: "Диалоги", note: "Сессия реплик", href: "/docs/adapters/dialogue-npc/", x: 20, y: 68 },
  { id: "npc", title: "NPC", note: "Актёр и сущность", href: "/docs/npc/materialization/", x: 42, y: 52 },
  { id: "adapter", title: "Адаптеры", note: "слой границы", href: "/docs/adapters/adapter-principle/", x: 62, y: 52 },
  { id: "animation", title: "Анимация", note: "субъекты и проигрывания", href: "/docs/animation/animation-overview/", x: 82, y: 36 },
  { id: "echoscript", title: "EchoScript", note: "Контентные ресурсы", href: "/docs/animation/animation-profiles/", x: 80, y: 70 },
  { id: "content", title: "Контент", note: "Пакеты и переопределения", href: "/docs/platform/content-workspace/", x: 52, y: 18 },
];

const links = [
  ["story", "npc"],
  ["dialogue", "npc"],
  ["npc", "adapter"],
  ["adapter", "animation"],
  ["content", "adapter"],
  ["echoscript", "animation"],
] as const;

function node(id: string) {
  return nodes.find((item) => item.id === id) ?? nodes[0];
}

export function RuntimeConstellation() {
  const [active, setActive] = useState("adapter");
  const selected = node(active);
  const connected = new Set(links.flatMap(([from, to]) => (from === active || to === active ? [from, to] : [])));

  return (
    <section class="runtime-constellation" aria-labelledby="home-title">
      <div class="home-title-block">
        <p>Echo Continuum</p>
        <h1 id="home-title">Платформа Echo</h1>
        <span>Наблюдайте, как история становится действием.</span>
      </div>

      <div class="runtime-map" aria-label="Интерактивная карта runtime">
        <svg viewBox="0 0 100 88" aria-hidden="true">
          {links.map(([fromId, toId]) => {
            const from = node(fromId);
            const to = node(toId);
            const isActive = fromId === active || toId === active;
            return (
              <path
                class={`runtime-link ${isActive ? "is-active" : ""}`}
                d={`M${from.x} ${from.y} C${(from.x + to.x) / 2} ${from.y - 12}, ${(from.x + to.x) / 2} ${to.y + 12}, ${to.x} ${to.y}`}
              />
            );
          })}
        </svg>
        {nodes.map((item) => (
          <a
            class="runtime-node"
            href={item.href}
            style={{ left: `${item.x}%`, top: `${item.y}%` }}
            data-active={active === item.id}
            data-connected={connected.has(item.id)}
            onMouseEnter={() => setActive(item.id)}
            onFocus={() => setActive(item.id)}
          >
            <strong>{item.title}</strong>
            <span>{item.note}</span>
            <em>Открыть</em>
          </a>
        ))}
      </div>

      <aside class="runtime-inspector" aria-label="Контекст выбранного узла">
        <span>Выбранный узел</span>
        <h2>{selected.title}</h2>
        <p>{selected.note}</p>
        <a href={selected.href}>Перейти к разделу</a>
      </aside>
    </section>
  );
}
