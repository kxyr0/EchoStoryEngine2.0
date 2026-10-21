import fs from "node:fs";
import path from "node:path";

const root = path.join(process.cwd(), "src", "content", "docs");
const updated = "2026-07-22";

const pages = [
  ["platform/overview", "Обзор системы", "Platform", "concept", ["DependencyBoundary", "ContractTable"]],
  ["platform/repository-map", "Карта репозиториев", "Platform", "architecture", ["DependencyBoundary"]],
  ["platform/id-system", "Система ID", "Platform", "reference", ["ContractTable"]],
  ["platform/dependency-rules", "Правила зависимостей", "Platform", "decision", ["DependencyBoundary"]],
  ["platform/server-authority", "Авторитет сервера", "Platform", "architecture", ["ProofPanel"]],
  ["platform/save-before-dispatch", "Сохранение перед отправкой", "Platform", "decision", ["ProofPanel"]],
  ["platform/content-workspace", "Рабочее пространство контента", "Platform", "reference", ["SyntaxLens"]],
  ["platform/console-commands", "Консольные команды", "Platform", "reference", ["CommandPlayground"]],
  ["platform/hot-reload", "Горячая перезагрузка", "Platform", "lifecycle", ["ProofPanel"]],
  ["animation/animation-overview", "Обзор анимации", "Animation", "concept", ["AnimationLifecycleLab"]],
  ["animation/a4-npc-lifecycle-integration", "Жизненный цикл NPC-анимации", "Animation", "lifecycle", ["NpcAnimationLifecycleMap", "SyntaxLens", "FailureMatrix", "ContractTable"]],
  ["animation/animation-profiles", "Профили анимации", "Animation", "syntax", ["SyntaxLens", "ContractTable"]],
  ["animation/npc-animation-bindings", "Привязки NPC-анимации", "Animation", "syntax", ["SyntaxLens", "ContractTable"]],
  ["animation/animation-ids", "ID анимации", "Animation", "reference", ["ContractTable"]],
  ["animation/channels-and-leases", "Каналы и lease", "Animation", "architecture", ["ContractTable"]],
  ["animation/segmented-humanoid", "Сегментированная модель", "Animation", "architecture", ["BoneRigExplorer"]],
  ["animation/rig-and-bones", "Rig и кости", "Animation", "reference", ["BoneRigExplorer"]],
  ["animation/clips", "Клипы", "Animation", "syntax", ["SyntaxLens"]],
  ["animation/graphs", "Графы", "Animation", "syntax", ["ContractTable"]],
  ["animation/whole-animations", "Цельные анимации", "Animation", "lifecycle", ["AnimationLifecycleLab"]],
  ["animation/restart-recovery", "Восстановление после перезапуска", "Animation", "lifecycle", ["RestartSimulator"]],
  ["animation/client-synchronization", "Синхронизация клиента", "Animation", "architecture", ["ProofPanel"]],
  ["animation/hot-reload", "Горячая перезагрузка анимации", "Animation", "lifecycle", ["ProofPanel"]],
  ["animation/diagnostics", "Диагностика", "Animation", "diagnostic", ["FailureMatrix"]],
  ["animation/authoring-tutorial", "Практика написания анимации", "Animation", "tutorial", ["SyntaxLens", "CommandPlayground"]],
  ["animation/full-reference", "Полный справочник", "Animation", "reference", ["ContractTable"]],
  ["npc/actor-id-vs-entity", "ActorId и сущность", "NPC", "concept", ["NpcAnimationLifecycleMap"]],
  ["npc/durable-actor-state", "Долговечное состояние актёра", "NPC", "lifecycle", ["ContractTable"]],
  ["npc/materialization", "Материализация", "NPC", "lifecycle", ["NpcAnimationLifecycleMap"]],
  ["npc/chunk-lifecycle", "Жизнь чанка", "NPC", "lifecycle", ["NpcAnimationLifecycleMap"]],
  ["npc/restart-reconstruction", "Реконструкция после restart", "NPC", "lifecycle", ["RestartSimulator"]],
  ["npc/animation-integration", "Интеграция анимации", "NPC", "architecture", ["NpcAnimationLifecycleMap", "DependencyBoundary"]],
  ["adapters/adapter-principle", "Принцип адаптера", "Adapters", "decision", ["DependencyBoundary"]],
  ["adapters/story-npc", "Story и NPC", "Adapters", "architecture", ["DependencyBoundary"]],
  ["adapters/dialogue-npc", "Dialogue и NPC", "Adapters", "architecture", ["DependencyBoundary"]],
  ["adapters/npc-animation", "NPC-анимация", "Adapters", "architecture", ["NpcAnimationLifecycleMap", "FailureMatrix"]],
  ["adapters/allowed-dependencies", "Разрешённые зависимости", "Adapters", "decision", ["DependencyBoundary"]],
  ["adapters/failure-isolation", "Изоляция отказов", "Adapters", "diagnostic", ["FailureMatrix"]],
];

const importMap = {
  AnimationLifecycleLab: "AnimationLifecycleLab",
  BoneRigExplorer: "BoneRigExplorer",
  CommandPlayground: "CommandPlayground",
  ContractTable: "ContractTable",
  DependencyBoundary: "DependencyBoundary",
  FailureMatrix: "FailureMatrix",
  NpcAnimationLifecycleMap: "NpcAnimationLifecycleMap",
  ProofPanel: "ProofPanel",
  RestartSimulator: "RestartSimulator",
  SyntaxLens: "SyntaxLens",
};

function frontmatter(title, section, pageKind, modules) {
  return `---
title: "${title}"
description: "${title} для платформы Echo."
section: "${section}"
pageKind: "${pageKind}"
status: "review"
since: "A4"
updated: "${updated}"
relatedIds: ["ActorId", "AnimationSubjectId", "AnimationProfileId"]
relatedPages: ["animation/a4-npc-lifecycle-integration", "platform/dependency-rules"]
sourceRepositories: ["echo-animation", "echo-npc", "echo-npc-animation-adapter", "echo-story-content-pack"]
sourcePaths: ["D:/JDHSeason/mods/echo-animation", "D:/JDHSeason/mods/echo-npc-animation-adapter"]
visualIdentity: "state-lines"
interactiveModules: [${modules.map((name) => `"${name}"`).join(", ")}]
searchKeywords: ["${title}", "A4", "EchoScript", "ActorId", "/echo_animation"]
---
`;
}

function componentImports(modules) {
  return [...new Set(modules)]
    .map((moduleName) => `import { ${importMap[moduleName]} } from "../../../components/${importMap[moduleName]}";`)
    .join("\n");
}

function moduleBlocks(modules, slug) {
  return modules.map((moduleName) => {
    if (moduleName === "SyntaxLens") {
      const kind = slug.includes("binding") || slug.includes("npc-animation") ? "binding" : "profile";
      return `<SyntaxLens example="${kind}" client:load />`;
    }
    return `<${importMap[moduleName]} client:load />`;
  }).join("\n\n");
}

function a4Body(title) {
  return `import { AnimationLifecycleLab } from "../../../components/AnimationLifecycleLab";
import { ContractTable } from "../../../components/ContractTable";
import { FailureMatrix } from "../../../components/FailureMatrix";
import { NpcAnimationLifecycleMap } from "../../../components/NpcAnimationLifecycleMap";
import { ProofPanel } from "../../../components/ProofPanel";
import { RestartSimulator } from "../../../components/RestartSimulator";
import { SyntaxLens } from "../../../components/SyntaxLens";

# ${title}

A4 автоматически связывает долговечных NPC-актёров с субъектами анимации. Адаптер читает публичные снимки NPC, выбирает авторские bindings, создаёт subjects через публичный Animation API и согласует пропущенные события на серверных тиках с лимитом 64 записи.

## Таблица жизненного цикла

| Состояние NPC | Сущность | Субъект анимации | Проигрывание |
| --- | --- | --- | --- |
| Durable ACTIVE, чанк загружен | Материализована | Должен существовать | Продолжается |
| Durable ACTIVE, чанк выгружен | Временно отсутствует | Сохранён | Продолжается на сервере |
| Durable ACTIVE, сущность восстановлена | Новый UUID | Тот же subject | Не перезапускает ENTER |
| Durable ABSENT | Отсутствует | Retired, если принадлежал адаптеру | Terminal SUBJECT_OWNER_REMOVED |
| UNBOUND | Может существовать | Не создаётся | Обычный renderer NPC |

## Схема архитектуры

\`\`\`mermaid
graph LR
  A["NpcDefinition"] --> B["ActorId"]
  B --> C["NpcAnimationBinding"]
  C --> D["AnimationProfileId"]
  D --> E["AnimationSubjectId"]
  E --> F["Сервер во время игры"]
  F --> G["Client Snapshot"]
\`\`\`

## Примеры профиля и binding

<SyntaxLens example="profile" client:load />

<SyntaxLens example="binding" client:load />

## Лаборатория жизненного цикла

<AnimationLifecycleLab client:load />

<NpcAnimationLifecycleMap client:load />

## Restart и смена профиля

<RestartSimulator client:load />

<ProofPanel client:load />

## Матрица отказов и публичный API

<FailureMatrix client:load />

<ContractTable client:load />

## Точные пути

- \`D:/JDHSeason/mods/echo-story-content-pack/src/main/resources/data/jdh/echo_animation/animation_profiles/chapter_1/family/standard_humanoid.animation-profile.es\`
- \`D:/JDHSeason/mods/echo-story-content-pack/src/main/resources/data/jdh/echo_npc_animation/bindings/chapter_1/family/john_father.npc-animation-binding.es\`
- \`C:/Users/kxyr0/AppData/Roaming/PrismLauncher/instances/Форма Мысли/minecraft/story/content-pack/jdh/echo_animation/animation_profiles/chapter_1/family/standard_humanoid.animation-profile.es\`
- \`C:/Users/kxyr0/AppData/Roaming/PrismLauncher/instances/Форма Мысли/minecraft/story/content-pack/jdh/echo_npc_animation/bindings/chapter_1/family/john_father.npc-animation-binding.es\`

## Команды

- \`/echo_animation play_animation jdh:wave jdh:john_father\`
- \`/echo_animation profiles\`
- \`/echo_npc_animation health\`
- \`/echo_npc_animation bindings\`
`;
}

function body(slug, title, section, modules) {
  if (slug === "animation/a4-npc-lifecycle-integration") {
    return a4Body(title);
  }
  return `${componentImports(modules)}

# ${title}

Эта страница фиксирует контракт раздела ${section} для темы «${title}». Она привязана к generated contracts и реальным EchoScript-примерам, а не к обещаниям без кода.

## Контракт во время игры

- Durable ID логические и имеют namespace.
- Серверное состояние авторитетно; клиенты получают снимки и изменения.
- Согласование идемпотентно и не должно сбрасывать активное проигрывание.
- Диагностика ограничена, чтобы один сломанный resource не ломал чужих актёров.

## Точки реализации

- \`D:/JDHSeason/mods/echo-animation\`
- \`D:/JDHSeason/mods/echo-npc\`
- \`D:/JDHSeason/mods/echo-npc-animation-adapter\`
- \`D:/JDHSeason/mods/echo-story-content-pack\`

${moduleBlocks(modules, slug)}
`;
}

for (const [slug, title, section, pageKind, modules] of pages) {
  const file = path.join(root, `${slug}.mdx`);
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, frontmatter(title, section, pageKind, modules) + body(slug, title, section, modules), "utf8");
}

console.log(`seed-docs wrote ${pages.length} MDX pages.`);
