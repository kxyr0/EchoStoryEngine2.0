# Dialogue DSL runtime contract

Use high-level `DialogueResource` for new content. `namespace`, `Id`, `Build`, `Init`, `Start`, `Alias`, `Nodes`,
`NodeDef`, `new Node`, and `return node` are not required.

Supported node-body operations retain their strict runtime meaning:

- `Line(speaker, text, key?, thought?, ...)` appends a line and preserves bounded cues.
- `LineAfterMove(...)` waits for the matching actor movement.
- `Choice(id, text, TargetNode)` creates a choice with a symbolic target.
- `GoTo(TargetNode)` creates an unconditional symbolic transition.
- `Complete("namespace:outcome")` creates an explicit terminal outcome.
- no explicit terminal creates the default completion after the final line.

Class metadata is sparse and typed:

- `Id("namespace:path")` overrides filename-derived identity;
- `Speaker(alias, actor: ..., display: ..., nameColor: ..., visual: ...)` overrides only supplied presentation fields;
- `Formerly("old_node_id", CurrentNode)` migrates a saved cursor explicitly.

The server-authoritative session engine still saves before presentation, advances by compiled cursor, and emits the
existing network messages. Dynamic presentation providers resolve current player appearance and current NPC form at
display time. Reload remains transactional: invalid sources do not replace the last-known-good catalog revision.

Legacy resources compile through the compatibility binder. New files should use `Node` / `start Node` so identity,
entry, graph edges, and source provenance are visible to tooling without boilerplate.
