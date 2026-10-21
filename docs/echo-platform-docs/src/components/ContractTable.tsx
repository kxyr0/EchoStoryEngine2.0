import { useMemo, useState } from "preact/hooks";
import animation from "../generated/contracts/echo-animation-contracts.json";
import adapter from "../generated/contracts/echo-npc-animation-adapter-contracts.json";
import npc from "../generated/contracts/echo-npc-contracts.json";

const rows = [
  { area: "Анимация", contract: "AnimationProfileId", source: animation.repository, rule: animation.contentPaths.externalProfiles },
  { area: "NPC", contract: "ActorId", source: npc.repository, rule: "долговечный логический identity, не UUID сущности" },
  { area: "Адаптер", contract: "NpcAnimationBindingId", source: adapter.repository, rule: adapter.echoScript.precedence.join(" > ") },
  { area: "Адаптер", contract: "Reconciliation", source: adapter.repository, rule: `${adapter.reconciliation.maxRecordsPerTick} records/tick` },
];

type SortKey = keyof (typeof rows)[number];

export function ContractMatrix() {
  const [filter, setFilter] = useState("");
  const [sort, setSort] = useState<SortKey>("contract");
  const visible = useMemo(
    () => rows
      .filter((row) => JSON.stringify(row).toLowerCase().includes(filter.toLowerCase()))
      .sort((a, b) => String(a[sort]).localeCompare(String(b[sort]), "ru")),
    [filter, sort],
  );

  return (
    <section class="interactive-panel contract-grid contract-matrix" aria-labelledby="contract-table-title" data-spine-label="Матрица контрактов">
      <div class="stage-panel-copy">
        <p class="eyebrow">сгенерированные контракты</p>
        <h3 id="contract-table-title">Матрица публичных контрактов</h3>
        <p>Факты подтягиваются из generated JSON и остаются связаны с исходными репозиториями.</p>
      </div>

      <div class="table-tools">
        <label>
          <span>Фильтр</span>
          <input value={filter} onInput={(event) => setFilter(event.currentTarget.value)} aria-label="Фильтр контрактов" placeholder="ID, репозиторий или правило" />
        </label>
        <div class="segmented" role="group" aria-label="Сортировка контрактов">
          {(["area", "contract", "source", "rule"] as SortKey[]).map((key) => (
            <button type="button" aria-pressed={sort === key} onClick={() => setSort(key)}>
              {key === "area" ? "Область" : key === "contract" ? "Контракт" : key === "source" ? "Источник" : "Правило"}
            </button>
          ))}
        </div>
      </div>

      <div class="matrix-scroll">
        <table>
          <caption>Сгенерированные факты публичных контрактов</caption>
          <thead>
            <tr><th>Область</th><th>Контракт</th><th>Источник</th><th>Правило</th></tr>
          </thead>
          <tbody>
            {visible.map((row) => (
              <tr id={`contract-${row.contract.toLowerCase()}`} data-lens-subject={row.contract}>
                <td>{row.area}</td>
                <td><code>{row.contract}</code></td>
                <td>{row.source}</td>
                <td>{row.rule}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

export const ContractTable = ContractMatrix;
