# EchoScript dialogue lowering

High-level dialogue syntax is parsed directly. There is no text preprocessing, macro expansion, reflection, dynamic
execution, or `Map<String, Object>` semantic layer.

The lowering pipeline is:

1. EchoScript parses optional namespace, class calls, and contextual `Node` declarations into sealed typed syntax.
2. `SourceResourceContextResolver` derives data namespace, file basename, logical source path, and inferred identity.
3. `DialogueResourceBinder` selects high-level or legacy binding without rewriting source text.
4. High-level passes normalize node symbols, validate collisions, determine the entry, read sparse speakers and
   `Formerly` mappings, bind node bodies, resolve symbolic transitions, and report reachability.
5. The result is the same strict `CompiledDialogue` consumed by the existing session engine, persistence, networking,
   presentation, hot reload, and last-known-good catalog.

`CompiledDialogue.SourceInfo` retains source ID, optional authored namespace, inference mode, authoring mode, and a
node-to-symbol/line/column map. Runtime modules depend only on this neutral provenance type, not on EchoScript parser
classes.

Identity inference is performed from the resource context, never from class names. Duplicate inferred IDs report
`ECHO_SCRIPT_RESOURCE_ID_COLLISION` and include both source paths. Graph diagnostics use stable codes such as
`ECHO_SCRIPT_DIALOGUE_ENTRY_MISSING`, `ECHO_SCRIPT_DIALOGUE_ENTRY_AMBIGUOUS`, `ECHO_SCRIPT_NODE_ID_COLLISION`, and
`ECHO_SCRIPT_NODE_NOT_FOUND`.

Lowering is deterministic: source member order does not affect node IDs, entry selection, edges, fingerprints, or
compiled meaning. An authored terminal-less node receives an explicit compiled completion so runtime behavior does
not depend on parser or catalog order.
