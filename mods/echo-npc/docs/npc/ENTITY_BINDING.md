# Entity Binding

`ActorId` - логическая identity actor. Она не является Minecraft entity UUID.

`EntityBindingId` - internal persisted incarnation id. Он связывает durable actor
record с конкретной acceptable physical entity.

## Rules

- Binding генерируется до platform spawn.
- Binding сохраняется в Actor Save Schema V1.
- Binding пишется в `EchoNpcEntity` NBT и synced data.
- Binding не показывается как public actor identity.
- Binding не derived from display name, position, hash code or entity UUID.

Recovery принимает loaded entity только при exact match:

```text
durable ActorId == entity ActorId
durable EntityBindingId == entity EntityBindingId
```

Entities с чужим actor id, пустым linkage или stale binding удаляются как
runtime projection. Unrelated Echo NPC рядом не удаляются при materialization.

N2 не использует Minecraft UUID как authority, потому что UUID принадлежит
physical entity incarnation and may change after reconstruction.
