import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";

const repoRoot = path.resolve(process.cwd());
const generatedRoot = path.join(repoRoot, "src", "generated");
const contractsRoot = path.join(generatedRoot, "contracts");
const examplesRoot = path.join(generatedRoot, "examples");

const sources = [
  {
    from: "D:/JDHSeason/mods/echo-animation/build/docs-export/echo-animation-contracts.json",
    to: path.join(contractsRoot, "echo-animation-contracts.json"),
  },
  {
    from: "D:/JDHSeason/mods/echo-npc/build/docs-export/echo-npc-contracts.json",
    to: path.join(contractsRoot, "echo-npc-contracts.json"),
  },
  {
    from: "D:/JDHSeason/mods/echo-npc-animation-adapter/build/docs-export/echo-npc-animation-adapter-contracts.json",
    to: path.join(contractsRoot, "echo-npc-animation-adapter-contracts.json"),
  },
  {
    from: "D:/JDHSeason/mods/echo-story-content-pack/src/main/resources/data/jdh/echo_animation/animation_profiles/chapter_1/family/standard_humanoid.animation-profile.es",
    to: path.join(examplesRoot, "standard_humanoid.animation-profile.es"),
  },
  {
    from: "D:/JDHSeason/mods/echo-story-content-pack/src/main/resources/data/jdh/echo_npc_animation/bindings/chapter_1/family/john_father.npc-animation-binding.es",
    to: path.join(examplesRoot, "john_father.npc-animation-binding.es"),
  },
];

function sha256(filePath) {
  return crypto.createHash("sha256").update(fs.readFileSync(filePath)).digest("hex");
}

fs.mkdirSync(contractsRoot, { recursive: true });
fs.mkdirSync(examplesRoot, { recursive: true });

const manifest = {
  generatedAt: new Date().toISOString(),
  sources: [],
};

for (const source of sources) {
  if (!fs.existsSync(source.from)) {
    throw new Error(`docs:sync source missing: ${source.from}`);
  }
  fs.copyFileSync(source.from, source.to);
  manifest.sources.push({
    from: source.from,
    to: path.relative(repoRoot, source.to).replaceAll("\\", "/"),
    sha256: sha256(source.from),
  });
}

fs.writeFileSync(
  path.join(generatedRoot, "manifest.json"),
  JSON.stringify(manifest, null, 2) + "\n",
  "utf8",
);

console.log(`docs:sync copied ${manifest.sources.length} contract/example files.`);
