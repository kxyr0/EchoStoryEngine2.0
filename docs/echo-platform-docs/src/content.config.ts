import { defineCollection, z } from "astro:content";
import { glob } from "astro/loaders";

const pageKind = z.enum([
  "concept",
  "architecture",
  "lifecycle",
  "syntax",
  "api",
  "tutorial",
  "diagnostic",
  "reference",
  "decision",
]);

const docs = defineCollection({
  loader: glob({ pattern: "**/*.{md,mdx}", base: "./src/content/docs" }),
  schema: z.object({
    title: z.string(),
    description: z.string(),
    section: z.enum(["Platform", "Animation", "NPC", "Adapters", "Trigger", "Player", "Wardrobe", "Effects", "Magic"]),
    pageKind,
    status: z.enum(["draft", "review", "stable"]),
    since: z.string(),
    updated: z.string(),
    relatedIds: z.array(z.string()),
    relatedPages: z.array(z.string()),
    sourceRepositories: z.array(z.string()),
    sourcePaths: z.array(z.string()),
    visualIdentity: z.string(),
    interactiveModules: z.array(z.string()),
    searchKeywords: z.array(z.string()),
  }),
});

export const collections = { docs };
