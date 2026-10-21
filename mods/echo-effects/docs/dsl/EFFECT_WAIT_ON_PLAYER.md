# Effect.WaitOnPlayer

Рабочая форма:

```csharp
Effect.WaitOnPlayer("jdh:first_morning/daze");
```

Это durable blocking command для host-а. Server thread не должен блокироваться; host должен возобновить script после terminal result конкретного instance lineage.
