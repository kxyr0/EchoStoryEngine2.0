# Echo Content Doctor

Doctor не разбирает EchoScript повторно. Каждый subsystem публикует immutable typed descriptor после собственного
parser/binder: resource identity, typed references, graph nodes, schedules, waits, leases, voice, aliases и precise
`SourceSpan`. `ContentGraph` индексирует `(kind, ResourceId)` и reverse dependencies за O(resources + references).

Errors ломают `contentDoctor`/`check`/`build`; warnings остаются в console и
`build/reports/echo-content-doctor.json`. Missing-reference suggestions ограничены тем же kind, затем сортируются по
namespace, edit distance и lexical order. Hot reload валидирует изменённый descriptor и reverse dependency closure.

EA1: AudioDescriptorProvider адаптирует AudioAssetScanner, общий с Echo Audio runtime.
AUDIO references поступают из Scene/Story compiler; missing sound даёт AUDIO_ASSET_NOT_FOUND
с подсказкой, ambiguous/unsupported assets — diagnostics сканера. Volume/pitch/duration/fade
проверяет общий AudioCommandBinder. Offline Doctor не декодирует Forge/Vorbis audio.

AudioActionSourceAdapter получает ссылки из Observation completion blocks через общий
AudioScriptInspection над существующим EchoScript AST. Это только audio-проверки,
полный Observation binder остаётся в собственном моде.
