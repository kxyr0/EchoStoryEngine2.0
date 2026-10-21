# Echo Debug source creation

Ключевые classes:

- `TriggerCreationService`
- `TriggerIdPlanner`
- `TriggerTargetPath`
- `TriggerSourceRenderer`
- `AtomicTriggerSourceWriter`
- `TriggerAuthoringServiceBridge`

## Source rendering

Generated skeleton содержит:

- `class <Name> : TriggerResource`;
- `private TriggerArea Area = ...`;
- `Id("namespace:path")`;
- `Scope(PLAYER)`;
- `Dimension("minecraft:...")`;
- `Bind(Area)`;
- `FireOnlyOnce()`;
- empty `OnTriggerEnter(PlayerRef player)`;
- empty `OnTriggerExit(TriggerEvent event)`.

Empty callbacks связываются production binder как reactive callbacks без action descriptors.
`StayInterval` не выводится, поэтому Stay scheduling не создаётся.

## Atomic write

Файл пишется во временный файл рядом с final path, выполняется `FileChannel.force(true)`, затем `ATOMIC_MOVE`.
Если reload после записи не подтвердил catalog, final file удаляется.
