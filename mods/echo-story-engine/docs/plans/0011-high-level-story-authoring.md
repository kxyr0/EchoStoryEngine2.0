# ES2 high-level StoryResource authoring

## Goal

Allow authored quests and stories to express ordered intent without `Build()`, `StoryResourceDef`, a manual
`Nodes()` graph, `next` links, or terminal plumbing.

## Canonical profile

```es
class FindNotebookQuest : StoryResource
{
    OnStart()
    {
        Story.Set("quests.find_notebook.accepted", true);
    }
}
```

The data-pack namespace and source basename infer the Story ID. `Id("namespace:path")` is the bounded override used
when an existing stable ID differs from the filename.

## Implementation

1. Resolve high-level Story identity from the Forge source ID.
2. Bind one `OnStart()` class-level intent section through the existing linear action lowering.
3. Infer deterministic start, action, and finish nodes.
4. Keep the legacy `Build()` plus `Nodes()`/method `OnStart()` profile readable.
5. Reject mixed legacy and high-level declarations.
6. Migrate production, acceptance, template, and authoring examples in Echo Story Content Pack.

## Verification

- Focused Story EchoScript adapter tests.
- Story Engine `check` and whitespace checks.
- Content pack validation and packaging without launching Minecraft or Forge.

