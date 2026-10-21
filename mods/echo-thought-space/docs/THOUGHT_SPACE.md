# Thought Space architecture and verification

## Lifecycle and input

The lifecycle is `CLOSED -> OPENING -> OPEN -> CLOSING -> CLOSED`. On the opening key edge, input capture is acquired before the non-pausing `ThoughtSpaceScreen` is installed. Every `KeyMapping` is released and queued clicks are drained; movement impulses are zeroed on every client tick. The screen consumes keyboard input, mouse buttons, and `Esc`. Only the configured toggle, cursor, LMB/RMB canvas pan, and wheel canvas zoom remain meaningful. Capture is released only after the closing fade reaches zero and the screen has been removed.

Opening is rejected when another screen owns input or when the optional cutscene integration reports an exclusive cinematic. No server movement freeze or Player Actor control lease is taken: the world continues ticking while human gameplay input is isolated locally.

## Canvas authority

`CanvasPoint`, `CanvasVector`, `CanvasCamera`, `CanvasViewport`, and `CanvasTransform` form a separate logical coordinate system. The viewport uses physical framebuffer dimensions as authority. GUI coordinates enter only at the mouse and draw boundaries and are explicitly converted. Node positions and the session camera therefore do not change when GUI Scale or resolution changes. Wheel zoom preserves the logical point below the cursor and pan divides physical drag by the complete responsive-plus-user zoom scale.

New node IDs receive deterministic golden-angle spiral slots. Existing ID-to-slot assignments survive provider refreshes for the client session. Rendering culls offscreen nodes, caches wrapped labels by node revision, and performs typed circular hit testing in physical pixels. Overlaps resolve by nearest center and then latest draw order. Nodes render as filled circles; hover smoothly enlarges and brightens the fill. Wrapped labels prefer a nearby position beneath or above their own node and keep a zoom-aware gap from every circle, including hover expansion, and previously placed labels. Labels are placed before connections and never move in response to a line. Permanent and dragged connections follow the shortest visible route around padded label bounds, with sampled quadratic curves rounding the turns. Unobstructed connections stay straight, and routing is cached for stationary endpoints and label bounds. Joined line quads use the same front-facing winding as GuiGraphics.fill and are explicitly flushed before nodes draw. A dragged endpoint inside a label stops at its protected boundary. Clear labels retain their usual position beneath the node.

## Combinations

LMB on empty canvas starts pan. RMB drag pans from anywhere, including nodes and labels; starting RMB pan cancels an active connection gesture. LMB on a node starts a connection gesture after the same six-physical-pixel drag threshold. The temporary line is rendered behind nodes. Releasing over a different node resolves an order-independent high-level `Combination` declaration from the hot-reloaded config catalog. A match unlocks exactly one derived provider node at half the accepted gesture arc length. The gesture is saved in canvas coordinates and split into two persistent input-to-result branches; their bends and side of text are retained, with only local detours for new labels. New result labels beside predominantly vertical gestures prefer a side of their circle so they do not immediately force the accepted path to detour. There is no hash-selected perpendicular result offset; derived nodes may be inputs to later recipes and survive provider refreshes and Thought Space reopenings for the current connected client session.

If there is no target or recipe, the endpoint returns to the source with a 280 ms cubic retraction while the line fades. Disconnecting clears session-derived nodes so discoveries do not leak between worlds or servers.

## Style source

Files ending in `.thought-space.es` below `story/configs` are discovered in deterministic path order. File metadata is checked at a 350 ms cadence only while Thought Space owns input; source text is parsed by the real EchoScript 0.3 ES2 frontend only when the matched file set, timestamp, or size changes. A typed binder accepts `ThoughtSpaceResource`, `Style`, immediate `Thought`, and derived `Combination` declarations. The renderer reads immutable snapshots and performs no per-frame DSL work. A malformed reload keeps the complete last-known-good snapshot.

## Provider boundary

Core packages contain no Observation imports. Providers expose immutable `ThoughtNodeId`, `ThoughtNodeLabel`, `ThoughtNodeRevision`, and `ThoughtNodeSnapshot` values. The optional Observation adapter reads only `ObservationClientView`; it publishes completed definitions with non-empty `Thought(...)`, deduplicates by generic node ID, and changes its provider revision when the catalog or completed set changes. `Text(...)` is intentionally ignored by this adapter.

## Manual matrix

Verify at 1280x720, 1920x1080, 2560x1440, ultrawide, fullscreen/windowed, and every GUI Scale:

1. Hold movement/attack/use, press `P`, and confirm zero movement or action on the first opening frame.
2. During `OPENING`, `OPEN`, and `CLOSING`, try movement, jump, sprint, inventory, hotbar keys, attack, use, and `Esc`; none may reach gameplay. The wheel must zoom the canvas without changing the hotbar.
3. Confirm the fade reaches true black, the world continues ticking, and `P` closes with capture held through the final transparent frame.
4. Pan with LMB, zoom over an off-centre node, and confirm that the canvas point under the cursor does not move. Change resolution and GUI Scale and confirm logical node positions remain stable.
5. Edit a `.thought-space.es` style while the canvas is open and confirm the visual values update without reopening. Introduce an invalid range and confirm the previous style remains active.
6. Complete an Observation with `Thought(...)`, reload, and confirm one node appears; change its thought/revision and confirm its label updates without moving.
7. Open a dialogue/menu/cutscene first and confirm Thought Space does not steal control.
8. Drag from a node to empty space and confirm the line retracts and fades. Drag between a configured pair in both directions and confirm only one result node appears.
9. Use the result as a later recipe input, close/reopen Thought Space, and confirm it remains until logout.
