# Echo Audio

Java-only root: `api` (resource/revision/playback identity, validated portable cue), `script`
(single AST binder), `asset` (authoritative scanner/descriptors, async bounded revision cache),
`pcm` (immutable PCM and decoder port), `playback` (gameplay envelope, owner/audience registry).

Forge module: `decode` adapts the public stateless echo-voice codec boundary without copying it.
`sound` streams cached PCM through Minecraft, downmixes positional playback to mono, loops the cursor.
`client` owns async presentation, resource reload and debug commands. `server` owns intent identities,
audience isolation and cleanup. `network` sends start/stop/end, never per-frame packets. `story`
registers an external action handler with bounded in-memory deduplication and interrupted recovery policy.

Dependency direction: audio root → EchoScript API only; audio Forge → root + Voice codecs (client)
+ Story extension contracts. Scene compiler → root binder; Scene Forge → AudioServer and existing
LocationCatalog. Story frontend → root binder; Story runtime/core remain independent of audio.
Doctor adapts audio scanner descriptors and compiler references without parsing audio/DSL again.

Common root library is packaged as the same Maven-identified Jar-in-Jar in Audio, Scene, Story
and Observation; Forge deduplicates it. Voice 0.2.1 bounds WAV/OGG PCM accumulation while reading.
Observation completion blocks use the same cue/binder, with a finite nonblocking lifecycle policy.
Verification/deployment evidence is recorded in the canonical guide and build/reports/ea1/release.json.

Runtime guide and audit: [AUDIO_DSL.md](../../../../docs/runtime/AUDIO_DSL.md).
