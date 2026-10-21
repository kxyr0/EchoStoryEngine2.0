# Voice hot reload architecture

The runtime chain is:

`WatchService` → `VoiceReloadDebouncer` → `VoiceFileStabilityProbe` → `VoiceDecodeQueue` → format decoder → PCM validation → client-thread `FilesystemVoiceAssetCatalog.commit` → new `VoiceAssetRevision` → `VoiceDecodedCache`.

Create, modify, delete, rename, and extension replacement events are reduced to `VoiceAssetKey`. Debounce defaults to 400 ms. Stability requires repeated equal size and modification time with bounded retries. A worker fully decodes and validates the candidate before the client thread commits it.

If zero candidates remain, the catalog entry is removed. If several supported extensions share a stem, no format wins and the previous entry remains. If decode or validation fails, the candidate is rejected and the last-known-good revision remains. Immediately before commit, the pipeline rechecks that exactly the decoded path is still the sole candidate.

An active playback owns a cache lease for its revision. Replacing or deleting the file therefore affects only future playback. MP3-to-WAV replacement keeps the same key and DSL, changes format, and increments revision after successful validation.
