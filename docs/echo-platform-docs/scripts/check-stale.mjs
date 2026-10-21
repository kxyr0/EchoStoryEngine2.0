import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";

const repoRoot = path.resolve(process.cwd());
const manifestPath = path.join(repoRoot, "src", "generated", "manifest.json");

if (!fs.existsSync(manifestPath)) {
  throw new Error("Generated docs contracts are missing. Run pnpm docs:sync.");
}

const manifest = JSON.parse(fs.readFileSync(manifestPath, "utf8"));
const stale = [];

for (const source of manifest.sources ?? []) {
  if (!fs.existsSync(source.from)) {
    stale.push(`${source.from}: source missing`);
    continue;
  }
  const current = crypto.createHash("sha256").update(fs.readFileSync(source.from)).digest("hex");
  if (current !== source.sha256) {
    stale.push(`${source.from}: source hash changed`);
  }
  const generated = path.join(repoRoot, source.to);
  if (!fs.existsSync(generated)) {
    stale.push(`${source.to}: generated copy missing`);
  }
}

if (stale.length > 0) {
  throw new Error(`Generated docs contracts are stale:\n${stale.join("\n")}\nRun pnpm docs:sync.`);
}

console.log(`docs:check-stale verified ${manifest.sources.length} generated files.`);
