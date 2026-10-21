import mdx from "@astrojs/mdx";
import preact from "@astrojs/preact";
import { defineConfig } from "astro/config";
import fs from "node:fs";

const echoScriptGrammar = JSON.parse(
  fs.readFileSync(new URL("./src/lib/echoscript.tmLanguage.json", import.meta.url), "utf8"),
);

export default defineConfig({
  output: "static",
  integrations: [mdx(), preact()],
  markdown: {
    shikiConfig: {
      themes: {
        light: "github-light",
        dark: "github-dark",
      },
      defaultColor: false,
      langs: [echoScriptGrammar],
    },
  },
  vite: {
    build: {
      rollupOptions: {
        output: {
          manualChunks(id) {
            return id.includes("/preact/") ? "interactive" : undefined;
          },
        },
      },
    },
  },
});
