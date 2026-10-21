namespace JDH.Effects;

class HeadSpinningEffect : EffectResource
{
    public override EffectResourceDef Build()
    {
        return Effect("jdh:body/head_spinning");
    }

    private void Init()
    {
        Duration(Seconds(7));
        Presentation(
            nameKey: "effect.jdh.body.head_spinning",
            descriptionKey: "effect.jdh.body.head_spinning.description",
            icon: "jdh:textures/echo_effects/icons/head_spinning.png"
        );
        Stacking(RESTART);
        Lifetime(INDEPENDENT);
        EndOn(PLAYER_DEATH, WORLD_EXIT, DISCONNECT);

        Movement.SpeedMultiplier(Curve(
            Key(Seconds(0.0), 1.0),
            Key(Seconds(0.4), 0.78, SMOOTH),
            Key(Seconds(5.8), 0.78),
            Key(Seconds(7.0), 1.0, SMOOTH)
        ));

        Camera.FovOffset(Curve(
            Key(Seconds(0.0), 0.0),
            Key(Seconds(0.5), -7.0, SMOOTH),
            Key(Seconds(5.8), -7.0),
            Key(Seconds(7.0), 0.0, SMOOTH)
        ));

        Camera.Roll(Curve(
            Key(Seconds(0.0), 0.0),
            Key(Seconds(0.8), 6.0, SMOOTH),
            Key(Seconds(2.2), -5.0, SMOOTH),
            Key(Seconds(3.8), 4.0, SMOOTH),
            Key(Seconds(5.4), -3.0, SMOOTH),
            Key(Seconds(7.0), 0.0, SMOOTH)
        ));

        Camera.Shake(
            amplitude: Curve(
                Key(Seconds(0.0), 0.0),
                Key(Seconds(0.4), 0.24, SMOOTH),
                Key(Seconds(5.8), 0.16),
                Key(Seconds(7.0), 0.0, SMOOTH)
            ),
            frequency: 5.5,
            seed: STABLE
        );

        Screen.Tint(
            color: Color("#6F748A"),
            opacity: Curve(
                Key(Seconds(0.0), 0.0),
                Key(Seconds(0.6), 0.20, SMOOTH),
                Key(Seconds(5.8), 0.20),
                Key(Seconds(7.0), 0.0, SMOOTH)
            ),
            layer: BELOW_UI
        );

        Screen.Vignette(
            color: Color("#10121C"),
            opacity: Curve(
                Key(Seconds(0.0), 0.0),
                Key(Seconds(0.6), 0.28, SMOOTH),
                Key(Seconds(5.8), 0.28),
                Key(Seconds(7.0), 0.0, SMOOTH)
            ),
            layer: BELOW_UI
        );

        Sound.PlayAt(
            time: Seconds(0.0),
            sound: "minecraft:block.amethyst_block.chime",
            volume: 0.22,
            pitch: 0.70
        );

        VanillaEffect.Apply(
            time: Seconds(0.0),
            effect: "minecraft:nausea",
            duration: Seconds(7.0),
            amplifier: 0,
            ambient: false,
            visible: true,
            showIcon: true
        );
    }
}
