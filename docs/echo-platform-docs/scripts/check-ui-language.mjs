import { readdirSync, readFileSync } from "node:fs";
import path from "node:path";

const banned = [
  "Overview",
  "Source Links",
  "Related IDs",
  "Search",
  "Previous",
  "Next",
  "Copy",
  "Close",
  "Open",
  "Operational Contract",
  "Implementation Anchors",
];

const dist = path.resolve("dist");
function collectHtml(dir, base = "") {
  return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const relative = path.join(base, entry.name);
    const absolute = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      return collectHtml(absolute, relative);
    }
    return entry.isFile() && entry.name.endsWith(".html") ? [relative] : [];
  });
}

const files = collectHtml(dist);
const failures = [];

for (const file of files) {
  const absolute = path.join(dist, file);
  let html = readFileSync(absolute, "utf8");
  html = html
    .replace(/\s(component-url|component-export|renderer-url|opts|uid|data-preact-island-id)="[^"]*"/gi, "")
    .replace(/<article[\s\S]*?<\/article>/gi, "")
    .replace(/<pre[\s\S]*?<\/pre>/gi, "")
    .replace(/<code[\s\S]*?<\/code>/gi, "")
    .replace(/<script[\s\S]*?<\/script>/gi, "")
    .replace(/<style[\s\S]*?<\/style>/gi, "");
  for (const phrase of banned) {
    if (html.includes(phrase)) {
      failures.push(`${file}: ${phrase}`);
    }
  }
}

if (failures.length > 0) {
  console.error("Found forbidden English UI strings:");
  for (const failure of failures) {
    console.error(`- ${failure}`);
  }
  process.exit(1);
}

console.log(`UI language check passed for ${files.length} rendered HTML files.`);
