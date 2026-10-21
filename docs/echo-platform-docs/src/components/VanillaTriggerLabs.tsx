import { useMemo, useState } from "preact/hooks";

const rows = [
  { name: "HasItem", kind: "predicate", category: "inventory", state: "inventory revision", result: "count" },
  { name: "ItemAdded", kind: "event", category: "inventory", state: "inventory delta", result: "item delta" },
  { name: "HoldingMainHand", kind: "predicate", category: "hands", state: "hand revision", result: "item id" },
  { name: "EquipmentEquipped", kind: "event", category: "equipment", state: "equipment delta", result: "slot" },
  { name: "PlayerDied", kind: "event", category: "player", state: "damage event", result: "death payload" },
  { name: "HasEffect", kind: "predicate", category: "effects", state: "effect revision", result: "amplifier" },
  { name: "TimeReached", kind: "event", category: "time", state: "world schedule", result: "world tick" },
  { name: "WeatherChanged", kind: "event", category: "weather", state: "weather diff", result: "weather" },
];

export function InventoryTriggerLab() {
  const [count, setCount] = useState(0);
  const [baseline, setBaseline] = useState(0);
  const added = count - baseline;
  return (
    <section class="interactive-panel" aria-labelledby="inventory-trigger-lab">
      <div class="stage-panel-copy">
        <p class="eyebrow">инвентарь</p>
        <h3 id="inventory-trigger-lab">HasItem и ItemAdded</h3>
      </div>
      <label>Алмазы <input type="number" min="0" value={count} onInput={(event) => setCount(Number(event.currentTarget.value))} /></label>
      <button type="button" onClick={() => setBaseline(count)}>Начать ожидание ItemAdded</button>
      <p>HasItem("minecraft:diamond"): {count > 0 ? "true" : "false"}</p>
      <p>ItemAdded("minecraft:diamond", count: 1): {added >= 1 ? "event" : "wait"}</p>
      <p>Baseline: {baseline}; delta: {added}</p>
    </section>
  );
}

export function EquipmentTriggerLab() {
  const [main, setMain] = useState("minecraft:air");
  const [head, setHead] = useState("minecraft:air");
  const wearing = head === "minecraft:carved_pumpkin";
  return (
    <section class="interactive-panel" aria-labelledby="equipment-trigger-lab">
      <div class="stage-panel-copy">
        <p class="eyebrow">руки и броня</p>
        <h3 id="equipment-trigger-lab">Predicate и equipment event</h3>
      </div>
      <label>Main hand <input value={main} onInput={(event) => setMain(event.currentTarget.value)} /></label>
      <label>HEAD <input value={head} onInput={(event) => setHead(event.currentTarget.value)} /></label>
      <p>HoldingMainHand("minecraft:diamond_sword"): {main === "minecraft:diamond_sword" ? "true" : "false"}</p>
      <p>Wearing(HEAD, "minecraft:carved_pumpkin"): {wearing ? "true" : "false"}</p>
    </section>
  );
}

export function PlayerStateTriggerLab() {
  const [health, setHealth] = useState(20);
  const [food, setFood] = useState(20);
  const [sneaking, setSneaking] = useState(false);
  return (
    <section class="interactive-panel" aria-labelledby="player-state-trigger-lab">
      <div class="stage-panel-copy">
        <p class="eyebrow">состояние игрока</p>
        <h3 id="player-state-trigger-lab">Health, food и flags</h3>
      </div>
      <label>Health <input type="range" min="0" max="20" value={health} onInput={(event) => setHealth(Number(event.currentTarget.value))} /></label>
      <label>Food <input type="range" min="0" max="20" value={food} onInput={(event) => setFood(Number(event.currentTarget.value))} /></label>
      <label><input type="checkbox" checked={sneaking} onInput={(event) => setSneaking(event.currentTarget.checked)} /> Sneaking</label>
      <p>HealthAtMost(6.0): {health <= 6 ? "true" : "false"}</p>
      <p>FoodAtLeast(10): {food >= 10 ? "true" : "false"}</p>
      <p>IsSneaking(): {sneaking ? "true" : "false"}</p>
    </section>
  );
}

export function VanillaTriggerReference() {
  const [kind, setKind] = useState("all");
  const [category, setCategory] = useState("all");
  const filtered = useMemo(() => rows.filter((row) => (kind === "all" || row.kind === kind)
    && (category === "all" || row.category === category)), [kind, category]);
  return (
    <section class="interactive-panel" aria-labelledby="vanilla-trigger-reference">
      <div class="stage-panel-copy">
        <p class="eyebrow">справочник</p>
        <h3 id="vanilla-trigger-reference">Фильтр выражений T2</h3>
      </div>
      <label>Kind <select value={kind} onInput={(event) => setKind(event.currentTarget.value)}>
        <option value="all">все</option><option value="predicate">predicate</option><option value="event">event</option>
      </select></label>
      <label>Category <select value={category} onInput={(event) => setCategory(event.currentTarget.value)}>
        <option value="all">все</option><option value="inventory">inventory</option><option value="equipment">equipment</option>
        <option value="player">player</option><option value="time">time</option><option value="weather">weather</option>
      </select></label>
      <ul>{filtered.map((row) => <li key={row.name}><code>{row.name}</code> — {row.kind}, {row.category}, {row.state}, {row.result}</li>)}</ul>
    </section>
  );
}
