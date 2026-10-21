# Echo Thought Space

Client-only Forge 1.20.1 mod that opens a non-pausing, fully black two-dimensional thought canvas. The default configurable key is `P`.

Completed Observation declarations that contain `Thought(...)` appear through the optional Observation adapter. The canvas itself consumes only immutable generic thought-node contracts, so further providers can register through `ThoughtNodeProviderRegistrationEvent` without depending on Observation internals.

Use LMB drag on empty canvas or RMB drag anywhere to pan and the mouse wheel to zoom around the cursor. ES2 `Thought Name() { Text(...); }` declarations provide immediate nodes; dragging one node to another resolves an order-independent `Combination Name() { From(...); Thought(...); }` and unlocks its result as a third node. The two input-to-result edges remain visible after a successful connection; a missing combination retracts the temporary line smoothly. Files ending in `.thought-space.es` under `story/configs` hot-reload through the real EchoScript frontend. See `examples/thought_space.thought-space.es`.

Commands:

- `/echo_thought_space status`
- `/echo_thought_space list`
- `/echo_thought_space debug on`
- `/echo_thought_space debug off`

`/echo_thought_space list` prints current node IDs and every declared `input + input -> result` identity, including locked results.

Build with `gradlew.bat check build`. The distributable is `build/libs/echo-thought-space-0.6.1-all.jar`.

Story unlock integration: when Echo Core >=0.2.0 is installed, access requires its per-player saved flag. See ../echo-core/README.md for commands, Story action examples, and persistence. Without Echo Core, standalone behavior is preserved.
