# Runtime Time DSL

These calls are bound by the real Story and Dialogue EchoScript compilers and executed by `TimeActionExecutor` on the server. Times are strict 24-hour `HH:mm`; days start at 1. Story action IDs and Dialogue wait keys derive durable mutation/wait IDs.

```echoscript
Time.Set("10:00");

Time.Set(
    Day(2),
    "08:30"
);

Time.Add(
    Minutes(10)
);

Time.Add(
    Hours(2),
    crossed: SKIP_CROSSED
);

Time.Pause();
Time.Resume();
Time.WaitUntil("10:00");
Time.Is("09:50");

Time.IsBetween(
    "23:00",
    "01:00"
);
```

`Time.Set` defaults to `SKIP_CROSSED`; `Time.Add` defaults to `FIRE_CROSSED`. `Time.WaitUntil` continues immediately when the requested time has not passed on the current day and the clock is already exactly there; otherwise it waits for the next matching absolute target. `Time.IsBetween("23:00", "01:00")` is a wrapping interval.

The available predicates are `Time.Is`, `Time.IsBefore`, `Time.IsAfter`, and `Time.IsBetween`. Story writes predicate result data to the action completion output map (`value=true|false`).

Trigger selectors use the existing Trigger DSL:

```echoscript
Subscribe(Time.At("09:50"));
Subscribe(Time.At(Day(2), "10:00"));
```

Both forms can target the existing `StartStory(...)` or `StartDialogue(...)` subscription actions. The canonical Story examples are covered by `EchoScriptTimeCompilerTest`, Dialogue examples by `EchoScriptDialogueTimeCompilerTest`, and both Trigger selector forms by `TriggerTimeSubscriptionBinderTest`; all tests call the production parser and binder.

