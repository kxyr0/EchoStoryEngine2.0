# Echo Voice project map

Runtime path:

`*.dialogue.es` → EchoScript parser → `DialogueResourceBinder` → `VoiceLineArgumentBinder` → `CompiledDialogueLine.Cue` → `ClientDialogueLifecycle` → `DialogueVoiceLifecycleBridge` → `VoiceAssetKey` → `FilesystemVoiceAssetCatalog` → `ForgeVoiceService` → `VoiceForegroundLane` → `VoiceDecoderRegistry` / `VoiceDecodedCache` → `VoiceSoundInstance` → Minecraft `SoundSource.VOICE`.

Repository modules:

- `voice-contracts`: immutable identity, asset, playback, receipt, and diagnostic records.
- `voice-core`: pure lane, reload, cache-key, and receipt policies.
- `voice-forge-api-1.20.1`: public service, catalog, query, and lifecycle boundaries.
- `voice-forge-1.20.1`: filesystem, codecs, cache, lane, commands, and Minecraft sound integration.
- `voice-testmod-1.20.1`: development-only acceptance mod; excluded from the production JAR.
- `echo-dialogue-voice-adapter`: separate public-API-only Dialogue integration repository.

Important source roots:

- `voice-forge-1.20.1/.../catalog/VoiceAssetPathResolver.java`: deterministic DialogueId-to-folder mapping.
- `voice-forge-1.20.1/.../reload/VoiceHotReloadPipeline.java`: stable candidate validation and atomic commit.
- `voice-forge-1.20.1/.../playback/lane/VoiceForegroundLane.java`: sole foreground owner.
- `voice-forge-1.20.1/.../playback/sound/VoiceStopRamp.java`: 8 ms click-removal ramp on the Minecraft sound executor.
- `echo-dialogue-voice-adapter/.../binding/VoiceLineArgumentBinder.java`: exact `voice: Voice(...)` syntax.
- `echo-dialogue-voice-adapter/.../lifecycle/DialogueVoiceLifecycleBridge.java`: committed line transitions only.
