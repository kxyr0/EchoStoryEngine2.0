# Scene runtime

Compiler использует публичный EchoScript parser и выдаёт `SceneDefinition` плюс `SceneDescriptor` для Content Doctor.
Каждый stage содержит одну последовательную команду или набор параллельных команд. Метаданные команды определяют
blocking mode и capabilities; runtime не содержит switch по gameplay-системам.

Runtime событийный: durable command возвращает waiting receipt, а продолжение вызывается completion-событием.
`SceneStepId` стабилен и сохраняется в checkpoint, поэтому recovery не повторяет завершённые `Time.Add` и другие
неидемпотентные шаги. Leases берутся лениво в порядке Player, Camera, NPC по ActorId и удерживаются до terminal state.
Cleanup адресуется точному `SceneExecutionId`: отменяются только owned commands, восстанавливается owned camera state и
освобождаются owned leases.

EA1: Sound.* компилируется общим AudioCommandBinder в portable AudioCue. AUDIO references
публикуются для Doctor; location symbols разрешаются существующим SceneSymbolCatalog.
AudioCommandAdapter отправляет intent через AudioServer и привязывает terminal ack к Scene step.
Cleanup останавливает только owned loops/blocking audio; one-shot продолжает естественно.
Location lookup event даёт Story доступ к тому же каталогу без второй location registry.
