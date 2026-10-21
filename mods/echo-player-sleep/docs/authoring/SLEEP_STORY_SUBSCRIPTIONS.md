# Sleep Story Subscriptions

Это обновление существующего Echo Player Sleep, а не новая система сна.

Канонический маршрут:

```echoscript
class FirstNightSleepSubscription : TriggerResource
{
    private PlayerSleepScenarioId FirstNight = "jdh:chapter_1/first_night";
    private StoryId FirstNightDream = "jdh:chapter_1/first_night_dream";

    private void Init()
    {
        Id("jdh:first_night_sleep");
        Subscribe(PlayerSleep.Confirmed(FirstNight));
        StartStory(FirstNightDream);
        FireOnlyOnce();
    }
}
```

Автор не пишет callback, не принимает `PlayerSleepEvent event`, не делает `if` по scenario и не передает session вручную.
`Subscribe(...)` компилируется в typed selector: provider `echo_player_sleep:events`, event kind и scenario id.
