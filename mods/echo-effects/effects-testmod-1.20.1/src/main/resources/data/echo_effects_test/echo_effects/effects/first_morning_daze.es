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
        EndOn(PLAYER_DEATH, WORLD_EXIT);

        Movement.SpeedMultiplier(Curve(
            Key(Seconds(0), 1.0),
            Key(Seconds(0.4), 0.82, SMOOTH),
            Key(Seconds(4.8), 0.82),
            Key(Seconds(6), 1.0, SMOOTH)
        ));

        Camera.FovOffset(Curve(
            Key(Seconds(0), 0.0),
            Key(Seconds(0.5), -8.0, SMOOTH),
            Key(Seconds(4.5), -8.0),
            Key(Seconds(6), 0.0, SMOOTH)
        ));

        Screen.Tint(
            color: Color("#6A7180"),
            opacity: Curve(
                Key(Seconds(0), 0.0),
                Key(Seconds(0.5), 0.24, SMOOTH),
                Key(Seconds(4.8), 0.24),
                Key(Seconds(6), 0.0, SMOOTH)
            ),
            layer: BELOW_UI
        );

        Screen.Vignette(
            color: Color("#11131A"),
            opacity: Curve(
                Key(Seconds(0), 0.0),
                Key(Seconds(0.5), 0.30, SMOOTH),
                Key(Seconds(4.8), 0.30),
                Key(Seconds(6), 0.0, SMOOTH)
            ),
            layer: BELOW_UI
        );

        Sound.PlayAt(
            time: Seconds(0),
            sound: "minecraft:block.beacon.ambient",
            volume: 0.25,
            pitch: 0.65
        );
    }
}
