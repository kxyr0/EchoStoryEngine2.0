# Public Service Bridge

`npc-forge-api-1.20.1` публикует typed event-based bridge.

## Получение service

```java
EchoNpcServiceLookupResult result = EchoNpcServiceBridge.lookup();
```

Возможные статусы:

- `RESOLVED` - найден ровно один provider.
- `UNAVAILABLE` - provider не зарегистрирован.
- `DUPLICATE_PROVIDER` - найдено несколько providers, service не возвращается.

## Зачем event bridge

Bridge сохраняет независимость modules и избегает ServiceLoader, static mutable
global service и reflection. Forge runtime регистрирует provider на event bus,
а другие mods остаются зависимыми только от `npc-forge-api-1.20.1` и
`npc-contracts`.
