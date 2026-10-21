# Примеры Magic

```text
private void Start()
{
    Magic.GrantToPlayer("example:bolt");
    Magic.SelectForPlayer("example:bolt");
    Magic.CastAsPlayer("example:bolt");
}
```

В реальном host вызовы связываются `MagicCommandBinder`; ближайший или случайный игрок не выбирается.
