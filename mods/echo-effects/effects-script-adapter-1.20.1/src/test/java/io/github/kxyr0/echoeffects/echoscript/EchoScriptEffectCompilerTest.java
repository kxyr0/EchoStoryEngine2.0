package io.github.kxyr0.echoeffects.echoscript;

import io.github.kxyr0.echoeffects.api.channel.EffectChannelId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class EchoScriptEffectCompilerTest {
    private final EchoScriptEffectCompiler compiler = new EchoScriptEffectCompiler();

    @Test
    void canonicalEffectResourceCompiles() {
        EffectCompileResult result = compiler.compile("jdh:first_morning_daze.es", """
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
                            Key(Seconds(6), 1.0, SMOOTH)
                        ));
                        Camera.FovOffset(Curve(Key(Seconds(0), 0.0), Key(Seconds(6), -8.0)));
                        Screen.Tint(
                            color: Color("#6A7180"),
                            opacity: Curve(Key(Seconds(0), 0.0), Key(Seconds(6), 0.24)),
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
                """);

        Assertions.assertTrue(result.successful(), result.diagnostics().toString());
        Assertions.assertEquals("jdh:first_morning/daze", result.definition().orElseThrow().id().value());
        Assertions.assertEquals("effect.jdh.first_morning.daze",
                result.definition().orElseThrow().presentation().nameKey());
        Assertions.assertEquals("effect.jdh.first_morning.daze.description",
                result.definition().orElseThrow().presentation().descriptionKey());
        Assertions.assertEquals("jdh:textures/echo_effects/icons/first_morning_daze.png",
                result.definition().orElseThrow().presentation().icon());
        Assertions.assertTrue(result.definition().orElseThrow().tracks().stream()
                .anyMatch(track -> EffectChannelId.MOVEMENT_SPEED.equals(track.channelId())));
        Assertions.assertEquals(1, result.definition().orElseThrow().timelineEvents().size());
    }

    @Test
    void headSpinningEffectResourceCompilesWithPresentation() {
        EffectCompileResult result = compiler.compile("jdh:head_spinning.es", """
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
                """);

        Assertions.assertTrue(result.successful(), result.diagnostics().toString());
        Assertions.assertEquals("jdh:body/head_spinning", result.definition().orElseThrow().id().value());
        Assertions.assertEquals("effect.jdh.body.head_spinning",
                result.definition().orElseThrow().presentation().nameKey());
        Assertions.assertEquals("effect.jdh.body.head_spinning.description",
                result.definition().orElseThrow().presentation().descriptionKey());
        Assertions.assertEquals("jdh:textures/echo_effects/icons/head_spinning.png",
                result.definition().orElseThrow().presentation().icon());
        Assertions.assertTrue(result.definition().orElseThrow().tracks().stream()
                .anyMatch(track -> EffectChannelId.CAMERA_ROLL.equals(track.channelId())));
        Assertions.assertTrue(result.definition().orElseThrow().tracks().stream()
                .anyMatch(track -> EffectChannelId.CAMERA_SHAKE.equals(track.channelId())));
        Assertions.assertTrue(result.definition().orElseThrow().timelineEvents().stream()
                .anyMatch(event -> EffectChannelId.VANILLA_MOB_EFFECT_APPLY.equals(event.channelId())
                        && "minecraft:nausea".equals(event.payload().get("effect"))
                        && "140".equals(event.payload().get("durationTicks"))));
        Assertions.assertEquals(2, result.definition().orElseThrow().timelineEvents().size());
    }
}
