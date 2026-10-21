# Writing NPCs

Файлы NPC лежат в resource path:

```text
data/<namespace>/echo_npc/npcs/**/*.npc.es
```

Пример:

```text
class Simple : NpcResource {
    public override NpcResourceDef Build() {
        return Npc(id: "sample:simple", display: "Житель");
    }
}
```

После reload все valid candidates компилируются в новый immutable catalog. Если
хотя бы один candidate дает error diagnostic, старый catalog остается активным.

## Правила ID

- Формат: `namespace:path`.
- Canonical form: lowercase.
- Максимальная длина public ID: 128 символов.
- `ActorId` задается отдельно при spawn и не должен извлекаться из entity UUID.

## Проверка в игре

```text
/reload
/echo_npc definitions
/echo_npc spawn sample:test sample:simple ~ ~ ~
/echo_npc actors
/echo_npc despawn sample:test
```
