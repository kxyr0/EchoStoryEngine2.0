# Echo Scene DSL

```es
class MorningKitchen : SceneResource
{
    Scene Start()
    {
        Time.Set("08:20");

        Parallel()
        {
            Father.MoveTo(KitchenTable);
            John.MoveTo(Chair);
        }

        Camera.Use(KitchenWide);
        Dialogue.Play("jdh:morning_with_father");
    }
}
```

`jdh:morning_kitchen` выводится из namespace content pack и имени файла, `start` — из имени Scene.
Команды последовательны; `Parallel` ждёт все ветви. Автор не пишет leases, `await`, `finally` или cleanup.
