import fs from "node:fs";
import path from "node:path";

const repoRoot = path.resolve(process.cwd());
const docsRoot = path.join(repoRoot, "src", "content", "docs");
const publicRoot = path.join(repoRoot, "public");

function frontmatter(text) {
  const match = text.match(/^---\n([\s\S]*?)\n---/);
  if (!match) {
    return {};
  }
  const data = {};
  for (const line of match[1].split("\n")) {
    const pair = line.match(/^([A-Za-z0-9_]+):\s*(.*)$/);
    if (!pair) {
      continue;
    }
    data[pair[1]] = pair[2].replace(/^"|"$/g, "");
  }
  return data;
}

function walk(dir) {
  if (!fs.existsSync(dir)) {
    return [];
  }
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const full = path.join(dir, entry.name);
    return entry.isDirectory() ? walk(full) : [full];
  });
}

const records = walk(docsRoot)
  .filter((file) => file.endsWith(".mdx"))
  .map((file) => {
    const text = fs.readFileSync(file, "utf8");
    const meta = frontmatter(text);
    const slug = path.relative(docsRoot, file).replace(/\\/g, "/").replace(/\.mdx$/, "");
    return {
      type: "Page",
      title: meta.title ?? slug,
      section: meta.section ?? "Platform",
      url: `/docs/${slug}/`,
      context: meta.description ?? "",
    };
  });

const commandRecords = [
  "/reload",
  "/echo_story health",
  "/echo_story diagnostics 20",
  "/echo_dialogue play jdh:chapter_1_home_arrival",
  "/echo_dialogue diagnostics 20",
  "/echo_npc spawn jdh:john_father jdh:john_father ~ ~ ~",
  "/echo_npc actor jdh:john_father",
  "/echo_npc_animation health",
  "/echo_npc_animation bindings",
  "/echo_animation play_animation jdh:wave jdh:john_father",
  "/echo_animation stop_animation jdh:john_father jdh:wave",
  "/echo_item validate",
].map((command) => ({
  type: "Command",
  title: command,
  section: "Commands",
  url: "/docs/platform/console-commands/",
  context: "Minecraft command syntax для проверки Echo content pack",
}));

fs.mkdirSync(publicRoot, { recursive: true });
fs.writeFileSync(
  path.join(publicRoot, "search-index.json"),
  JSON.stringify([...records, ...commandRecords], null, 2) + "\n",
);

console.log(`docs:build-search indexed ${records.length + commandRecords.length} fallback records.`);
