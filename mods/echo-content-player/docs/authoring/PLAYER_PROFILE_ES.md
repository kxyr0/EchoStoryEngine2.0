# Шаблоны профиля через EchoScript

Шаблон профиля — обычный `.player.es`, который проходит реальный EchoScript parser и затем player-specific binder. Echo Content Player не исполняет EchoScript как код; он читает immutable AST и извлекает разрешённые поля.

Минимальный пример:

```csharp
namespace EchoContentPlayer.Jdh;

class DefaultPlayerProfile : PlayerProfileTemplate
{
    public override PlayerProfile Build()
    {
        return PlayerProfile(
            id: "jdh:player/default",
            nameMode: ACCOUNT,
            spark: 100,
            maxSpark: 100,
            skinMode: ACCOUNT
        );
    }
}
```

Разрешённые поля P1: `id`, `nameMode`, `displayName`, `spark`, `maxSpark`, `skinMode`, `skinId`, `skinModel`.

`CUSTOM` требует `displayName`. `RESOURCE` требует `skinId` и `skinModel`. Некорректные значения дают bounded diagnostics и не должны ломать hot reload.
