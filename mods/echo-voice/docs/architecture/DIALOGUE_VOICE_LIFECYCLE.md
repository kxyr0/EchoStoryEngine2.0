# Dialogue voice lifecycle

Echo Dialogue publishes client-safe events only after authoritative presentation state commits. The adapter listens to those events and never listens to raw mouse or keyboard input.

`LINE_ENTER_COMMITTED` with valid voice cues creates one generation-tagged `VoicePlaybackRequest`. Duplicate and stale presentation events are idempotently rejected. `LINE_EXIT_COMMITTED` maps its typed reason to `VoiceStopReason` and stops the current lane. Dialogue replacement posts old exit before new enter.

`TYPEWRITER_REVEAL` updates adapter diagnostics only. It does not call play or stop, so playback identity and position remain unchanged. Resize, GUI scale, fullscreen, and UI renderer reload publish no line transition and therefore do nothing to voice.

Lifecycle sequence:

`Line A enter` → `A start` → `typewriter reveal` → `A continues` → `Line A exit committed` → `A ramp/stop` → `A stop confirmed` → `Line B enter` → `B start`.

Receipts are keyed by Dialogue session, presentation revision, and asset key. A completed current line does not replay after reconnect. A voice interrupted by disconnect may restart once when the same line is authoritatively presented again. Voice completion never advances Dialogue.
