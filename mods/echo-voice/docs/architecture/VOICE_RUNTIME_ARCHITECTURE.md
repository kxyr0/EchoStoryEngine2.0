# Voice runtime architecture

EA1 / 0.2.1: WAV and OGG now use bounded PCM chunk accumulation through VoicePcmReadLimit,
rejecting overlong decoded output before loading the entire stream. MP3 already checks each frame.
Echo Audio reuses the existing public codec boundary; Voice lanes, channel and receipts are unchanged.
Изменения: [реестр](../changelogs/README.md).

The runtime identity is `VoiceAssetKey(DialogueId, VoiceFileStem)`. File extension and absolute path are never part of identity. `VoiceAssetPathResolver` accepts basename stems only and rejects path traversal.

`VoiceCatalogScanner` indexes metadata at startup without decoding the season. `ForgeVoiceService` submits cold decodes to the bounded priority `VoiceDecodeQueue`; workers receive immutable paths and limits and never access Minecraft world, Dialogue state, rendering, or OpenAL. WAV uses Java Sound, MP3 uses pinned JLayer 1.0.1, and OGG uses Minecraft's Vorbis decoder. All outputs become signed 16-bit little-endian mono/stereo `DecodedPcm`.

`VoiceDecodedCache` is a byte-bounded LRU keyed by asset key plus revision. `VoiceCacheLease` pins active and preloaded PCM. A new valid revision does not disturb an active old lease.

`VoiceForegroundLane` owns at most one `VoiceSoundInstance`. A pending request is delayed to a later client tick, so intermediate same-update presentations never become audible. Replacement first requests an 8 ms hard-stop ramp, waits until Minecraft reports the old source inactive, and only then starts the pending source. `VoiceStopRamp` changes gain and stops on Minecraft's sound executor; it is not a crossfade. Playback remains routed through `SoundSource.VOICE`, so Master and Voice category changes apply live.

Gameplay public views expose immutable metadata only. OpenAL channels and mutable Minecraft sound
internals stay within client presentation. The stateless VoiceAudioDecoder/VoiceDecoderRegistry →
DecodedPcm boundary is deliberately shared with Echo Audio through its client codec adapter;
PCM is never exposed to EchoScript, server payloads, or Voice gameplay receipts.
