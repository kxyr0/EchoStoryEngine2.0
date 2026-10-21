# Magic gesture casting

Echo Magic 0.3.0 separates school selection from technique casting. Holding the configured key starts a client gesture session; releasing it finishes the path. No cast packet is sent on key press.

The client projects camera aim onto a fixed plane 2.25 blocks in front of the initial eye position. Captured 2D points render as a local world-space dust trail. A path contains at most 96 client points and lasts at most five seconds.

The client recognizes school shapes plus `LINE`, `ARC` and large `CIRCLE` techniques for immediate feedback, then sends the bounded point list. `MagicGestureRecognizer` and `MagicTechniqueRecognizer` run again on the server. `MagicGestureSequence` resolves whether the path selects a school or casts a technique.

Initial symbols:

| Symbol | School path | Shape |
| --- | --- | --- |
| Fire | `fire` | upward stroke `↑` |
| Water | `water` | horizontal wave `~` |
| Earth | `earth` | downward stroke with a foot `⊥` |
| Air | `air` | closed loop `○` |
| Blood | `blood` | point held for at least 450 ms `•` |
| Void | `void` | linear outward stroke erased by returning over it `↔` |

The first school symbol selects a school and stores it in `MagicPlayerSavedData`; it never casts immediately. A later straight line casts inside the remembered school. The server filters the player's known spells by the path of `SpellDefinition.school()`. If the current selected spell belongs to that school it wins; otherwise the first known spell in that school is used. This keeps the generic engine independent of Fire, Void, Water, Air, Blood and future content mods.

An upward line is intentionally contextual. A deliberate, nearly vertical Fire stroke must last at least 320 ms; a fast line stays a technique for the remembered school. When Fire is already active, the same straight stroke casts Fire. Horizontal and diagonal lines cast any remembered school. A line with no remembered school is rejected with `GESTURE_SCHOOL_REQUIRED`. Built-in school recognizers use closure, direction-change and return-path limits. Personal templates use rotation-tolerant dynamic path alignment instead of an exact topology gate. School input may span consecutive LMB strokes; the server merges them before recognition so releasing LMB at the Void turnaround does not discard the return stroke.

Technique routing uses `SpellDefinition.technique()`, authored as `Technique(LINE)`, `Technique(ARC)` or `Technique(CIRCLE)`. A broad single-sided curve is an arc; a large closed loop is a circle. The large-circle threshold is deliberately above the small Air school loop. If a player already knows a school, the first use of a newly installed technique spell from that school grants it automatically before casting.

`slow_time_while_drawing` is a client toggle. While enabled, the client announces an active drawing session and the integrated server adds 100 ms to each server tick. Rendering and camera input stay responsive. Dedicated servers ignore this request unless `allow_gesture_slowdown_on_dedicated_server=true` is explicitly set by the server owner. Drawing state expires after eight seconds if a release packet is lost.
