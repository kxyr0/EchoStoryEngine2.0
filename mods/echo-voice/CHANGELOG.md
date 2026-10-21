# Changelog

## 0.2.1

- WAV и OGG проверяют ограничение PCM во время чтения, до полной загрузки длинного файла.
- Echo Audio переиспользует тот же decoder boundary; речевые очереди не менялись.

## 0.2.0

- Added a bounded, low-priority positional ambient voice lane for background conversations.
- Reused the existing catalog, decoder queue, and PCM cache without creating a second audio pipeline.
- Foreground dialogue now preempts ambient playback, and ambient requests are range-checked before decode.

## 0.1.0

- Initial WAV, MP3, and OGG foreground Dialogue voice runtime.
- Strict stop-confirm-before-start lane, bounded decode/cache, hot reload, diagnostics, and public lifecycle API.
