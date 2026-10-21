import { useMemo, useState } from "preact/hooks";
import bindingSource from "../generated/examples/john_father.npc-animation-binding.es?raw";
import profileSource from "../generated/examples/standard_humanoid.animation-profile.es?raw";

interface SyntaxLensProps {
  example: "profile" | "binding";
}

const modes = ["Исходник", "AST", "Связано", "Во время игры"] as const;

const explanations: Record<string, {
  title: string;
  source: string;
  ast: string;
  bound: string;
  runtime: string;
}> = {
  AnimationProfile: {
    title: "AnimationProfile",
    source: "Определяет ресурс профиля анимации.",
    ast: "ClassDeclaration -> Build() -> AnimationProfile(...)",
    bound: "Binder создаёт AnimationProfileId и проверяет rig/graph.",
    runtime: "Во время игры профиль используется при ensure subject.",
  },
  NpcAnimationBinding: {
    title: "NpcAnimationBinding",
    source: "Привязывает NPC к профилю анимации.",
    ast: "BindingResource -> npc/profile fields",
    bound: "Binder сверяет ActorId или NpcDefinitionId с AnimationProfileId.",
    runtime: "Адаптер создаёт subject только для связанного actor.",
  },
  initialStates: {
    title: "initialStates",
    source: "Начальные параметры subject при первом создании.",
    ast: "ObjectField(initialStates)",
    bound: "Применяется только при safe profile bind.",
    runtime: "Не должен сбрасывать существующий playback.",
  },
  requiredChannels: {
    title: "requiredChannels",
    source: "Каналы, без которых playback нельзя запускать.",
    ast: "ArrayLiteral(ChannelId)",
    bound: "Проверка доступности body/face/arm channels.",
    runtime: "Lease блокирует конфликтующие проигрывания.",
  },
  profile: {
    title: "profile",
    source: "Ссылка binding на AnimationProfileId.",
    ast: "IdentifierReference(profile)",
    bound: "Resolver ищет профиль в Echo Animation catalog.",
    runtime: "Адаптер передаёт profileId в provisioning call.",
  },
  npc: {
    title: "npc",
    source: "Definition-level binding для группы NPC.",
    ast: "IdentifierReference(npc)",
    bound: "Resolver может заменить definition binding через переопределение actor.",
    runtime: "ActorId получает subject без зависимости animation -> NPC.",
  },
};

function pickKey(line: string) {
  return Object.keys(explanations).find((name) => line.includes(name)) ?? "source";
}

function fallback(line: string) {
  return {
    title: line.trim() || "пустая строка",
    source: "Структурная строка EchoScript.",
    ast: "Parser сохраняет позицию для diagnostics.",
    bound: "Binder пропускает строку без domain-symbol.",
    runtime: "Во время игры состояние напрямую не меняется.",
  };
}

export function SyntaxXRay({ example }: SyntaxLensProps) {
  const source = example === "profile" ? profileSource : bindingSource;
  const [line, setLine] = useState(1);
  const [mode, setMode] = useState<(typeof modes)[number]>("Исходник");
  const lines = useMemo(() => source.split(/\r?\n/), [source]);
  const selected = lines[line - 1] ?? "";
  const key = pickKey(selected);
  const detail = explanations[key] ?? fallback(selected);
  const modeText = mode === "Исходник" ? detail.source : mode === "AST" ? detail.ast : mode === "Связано" ? detail.bound : detail.runtime;

  return (
    <section class="interactive-panel syntax-xray" aria-labelledby={`syntax-${example}`} data-spine-label="Синтаксический рентген">
      <div class="stage-panel-copy">
        <p class="eyebrow">EchoScript</p>
        <h3 id={`syntax-${example}`}>Синтаксический рентген: {example === "profile" ? "профиль" : "привязка"}</h3>
        <p>Выбор строки показывает путь от исходника через AST и binder к контракту во время игры.</p>
      </div>

      <div class="syntax-source">
        <pre class="syntax-lines" aria-label="Исходный EchoScript файл">
          {lines.map((text, index) => (
            <button
              type="button"
              class="syntax-line"
              data-selected={line === index + 1}
              onClick={() => setLine(index + 1)}
              aria-label={`Пояснить строку ${index + 1}`}
            >
              <span>{index + 1}</span>
              <span>{text || " "}</span>
            </button>
          ))}
        </pre>
      </div>

      <aside class="syntax-readout">
        <div class="segmented" role="group" aria-label="Режим семантической линзы">
          {modes.map((item) => (
            <button type="button" aria-pressed={mode === item} onClick={() => setMode(item)}>
              {item}
            </button>
          ))}
        </div>
        <p class="line-index">Строка {line}</p>
        <h4>{detail.title}</h4>
        <p>{modeText}</p>
        <dl>
          <div>
            <dt>AST</dt>
            <dd><code>{detail.ast}</code></dd>
          </div>
          <div>
            <dt>Контракт во время игры</dt>
            <dd><code>{detail.runtime}</code></dd>
          </div>
        </dl>
      </aside>
    </section>
  );
}

export const SyntaxLens = SyntaxXRay;
