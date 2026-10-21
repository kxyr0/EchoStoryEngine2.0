# EffectResource Reference

An effect resource is an EchoScript class that extends `EffectResource`.

```csharp
namespace JDH.Effects;

class FirstMorningDazeEffect : EffectResource
{
    public override EffectResourceDef Build()
    {
        return Effect("jdh:first_morning/daze");
    }

    private void Init()
    {
        Duration(Seconds(6));
        Presentation(
            nameKey: "effect.jdh.first_morning.daze",
            descriptionKey: "effect.jdh.first_morning.daze.description",
            icon: "jdh:textures/echo_effects/icons/first_morning_daze.png"
        );
        Stacking(RESTART);
        Lifetime(INDEPENDENT);
        Movement.SpeedMultiplier(Curve(Key(Seconds(0), 1.0), Key(Seconds(6), 1.0)));
    }
}
```

The runtime accepts only declarative channels. It does not execute arbitrary Java callbacks from `.es`.

`Presentation(...)` is optional, but content effects should set it so UI/debug views can show a localized name, a localized description, and an icon.
