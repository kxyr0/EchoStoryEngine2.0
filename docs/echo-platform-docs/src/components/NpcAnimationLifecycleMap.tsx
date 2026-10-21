import { useState } from "preact/hooks";

const states = [
  {
    id: "ACTIVE",
    step: "активный актёр",
    label: "Активен",
    note: "Сущность существует, субъект анимации закреплён за ActorId.",
    entity: "UUID-a12",
    bits: { actor: true, entity: true, subject: true, playback: true, lease: true, client: true },
  },
  {
    id: "MATERIALIZED",
    step: "есть сущность",
    label: "Материализован",
    note: "Снимок актёра превращён в физическую сущность Minecraft.",
    entity: "UUID-a12",
    bits: { actor: true, entity: true, subject: true, playback: false, lease: false, client: true },
  },
  {
    id: "CHUNK_UNLOADED",
    step: "чанк выгружен",
    label: "Чанк выгружен",
    note: "Сущность исчезла из мира, но логические ID и состояние проигрывания сохранены.",
    entity: "нет сущности",
    bits: { actor: true, entity: false, subject: true, playback: true, lease: true, client: false },
  },
  {
    id: "REMATERIALIZED",
    step: "возвращение",
    label: "Возвращён",
    note: "Новая сущность получает старый ActorId и тот же AnimationSubjectId.",
    entity: "UUID-f91",
    bits: { actor: true, entity: true, subject: true, playback: true, lease: true, client: true },
  },
  {
    id: "TELEPORTED",
    step: "смена позиции",
    label: "Телепортирован",
    note: "Позиция меняется, логический identity не меняется.",
    entity: "UUID-f91",
    bits: { actor: true, entity: true, subject: true, playback: true, lease: true, client: true },
  },
  {
    id: "ABSENT",
    step: "актёра нет",
    label: "Отсутствует",
    note: "Актёр не загружен; адаптер не создаёт скрытый запасной subject.",
    entity: "нет сущности",
    bits: { actor: false, entity: false, subject: false, playback: false, lease: false, client: false },
  },
];

const rows = [
  ["actor", "Актёр", "jdh:john_father"],
  ["entity", "Сущность", "UUID сущности"],
  ["subject", "Субъект анимации", "jdh:john_father/body"],
  ["playback", "Проигрывание", "jdh:wave#main"],
  ["lease", "Захват канала", "канал body"],
  ["client", "Подписка клиента", "1 клиент"],
] as const;

export function NpcLifecycleMap() {
  const [selected, setSelected] = useState(states[0].id);
  const state = states.find((item) => item.id === selected) ?? states[0];

  return (
    <section class="interactive-panel npc-lifecycle-map" aria-labelledby="npc-life-title" data-spine-label="Карта жизненного цикла NPC">
      <div class="stage-panel-copy">
        <p class="eyebrow">адаптер NPC-анимации</p>
        <h3 id="npc-life-title">Карта жизненного цикла NPC</h3>
        <p>UUID сущности может измениться, но ActorId и AnimationSubjectId остаются закреплёнными.</p>
      </div>

      <div class="npc-state-strip" role="group" aria-label="Состояние NPC">
        {states.map((item) => (
          <button type="button" aria-pressed={selected === item.id} onClick={() => setSelected(item.id)}>
            <span>{item.step}</span>
            <strong>{item.label}</strong>
          </button>
        ))}
      </div>

      <div class="npc-runtime-map">
        <div class="identity-column">
          <span>ActorId</span>
          <code>jdh:john_father</code>
          <span>Сущность</span>
          <code>{state.entity}</code>
          <span>AnimationSubjectId</span>
          <code>jdh:john_father/body</code>
        </div>
        <div class="runtime-grid">
          {rows.map(([key, label, value]) => (
            <button type="button" class="runtime-cell" data-active={state.bits[key]} data-lens-subject={label}>
              <strong>{label}</strong>
              <span>{state.bits[key] ? "активно" : "нет"}</span>
              <code>{value}</code>
            </button>
          ))}
        </div>
      </div>

      <p class="state-note">{state.note}</p>
    </section>
  );
}

export const NpcAnimationLifecycleMap = NpcLifecycleMap;
