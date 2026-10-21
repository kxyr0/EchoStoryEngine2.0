# High-level DialogueResource

The canonical authoring path is:

`data/<namespace>/echo_dialogue/dialogues/**/*.dialogue.es`

The smallest valid resource is intentionally just the story meaning:

```csharp
class UniversityRememberDialogue : DialogueResource
{
    Node FirstTimeUniversity()
    {
        Line("john", "Кажется пора идти на пары...", thought: true);
    }
}
```

For `data/jdh/echo_dialogue/dialogues/university_remember.dialogue.es`, the compiled identity is
`jdh:university_remember`. The data namespace and file basename define identity. EchoScript `namespace`, class name,
and containing folders are organizational and do not change it. Renaming the file does change identity. `Id(...)` is
the explicit stable override.

## Graph rules

- One `Node` is the implicit entry.
- Two or more nodes require exactly one `start Node`.
- Declaration order has no graph meaning.
- Node IDs are normalized from symbols: `FirstTimeUniversity` becomes `first_time_university`.
- `NodeId("...")` is the bounded explicit override for a node.
- `GoTo(TargetNode)` is a symbol reference. Missing targets and normalization collisions are compile errors.
- A node without `Choice`, `GoTo`, or `Complete` receives a deterministic implicit completion.
- Empty nodes and unreachable nodes are warnings; an empty resource is an error.

Example graph:

```csharp
class UniversityDay : DialogueResource
{
    start Node Entrance()
    {
        Line("john", "Пора на пары.");
        GoTo(Hall);
    }

    Node Hall()
    {
        Line("john", "Я на месте.");
    }
}
```

## Speakers and current appearance

`Line("john", ...)` lowers to the typed speaker identity `jdh:john` in a `jdh` resource. At presentation time the
registered player/NPC provider resolves the actor's current name, skin, portrait, form, and animation state. Dialogue
source therefore does not duplicate appearance data.

An optional class-level `Speaker(...)` is a sparse override:

```csharp
Speaker("john", actor: "jdh:john", display: "Неизвестный");
```

Only authored fields override the provider. Unspecified display, name color, and visual continue to come from the
current actor presentation.

## Save migrations

Node renames are never guessed. Declare an explicit mapping when an old saved cursor must survive a rename:

```csharp
Formerly("greeting", UniversityEntrance);
```

The recovery planner applies this mapping before fingerprint validation. Without a mapping, incompatible snapshots
remain blocked rather than silently jumping to a different node.

Legacy `Build`, `Init`, `Alias`, `Nodes`, `NodeDef`, and string-based starts remain supported by the legacy binder.
High-level and legacy graph declarations cannot be mixed in one resource.
