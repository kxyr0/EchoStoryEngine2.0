namespace EchoAnimation.Tests;

class FullHumanoidClip : AnimationClipResource
{
    public override AnimationClipDef Build()
    {
        return AnimationClip("echo_animation:full_humanoid_test",
            rig: "echo_animation:tests/full_humanoid",
            tracks: [
                Track("root", frames: [
                    Frame("0.0").Position(0, "0.15", 0),
                    Frame("0.25", interpolation: "SMOOTH").Position(0, "-0.35", 0),
                    Frame("0.5", interpolation: "SMOOTH").Position(0, "0.15", 0),
                    Frame("0.75", interpolation: "SMOOTH").Position(0, "-0.35", 0),
                    Frame("1.0", interpolation: "SMOOTH").Position(0, "0.15", 0)
                ]),
                Track("body", frames: [
                    Frame("0.0").Rotation("-4.0", 0, "-2.0"),
                    Frame("0.25", interpolation: "SMOOTH").Rotation("-3.0", 0, 0),
                    Frame("0.5", interpolation: "SMOOTH").Rotation("-4.0", 0, "2.0"),
                    Frame("0.75", interpolation: "SMOOTH").Rotation("-3.0", 0, 0),
                    Frame("1.0", interpolation: "SMOOTH").Rotation("-4.0", 0, "-2.0")
                ]),
                Track("head", frames: [
                    Frame("0.0").Rotation("2.0", "-5.0", "2.0"),
                    Frame("0.25", interpolation: "SMOOTH").Rotation(0, 0, 0),
                    Frame("0.5", interpolation: "SMOOTH").Rotation("2.0", "5.0", "-2.0"),
                    Frame("0.75", interpolation: "SMOOTH").Rotation(0, 0, 0),
                    Frame("1.0", interpolation: "SMOOTH").Rotation("2.0", "-5.0", "2.0")
                ]),
                Track("left_upper_arm", frames: [
                    Frame("0.0").Rotation("-28.0", 0, "-3.0"),
                    Frame("0.25", interpolation: "SMOOTH").Rotation(0, 0, "-2.0"),
                    Frame("0.5", interpolation: "SMOOTH").Rotation("28.0", 0, "-3.0"),
                    Frame("0.75", interpolation: "SMOOTH").Rotation(0, 0, "-2.0"),
                    Frame("1.0", interpolation: "SMOOTH").Rotation("-28.0", 0, "-3.0")
                ]),
                Track("right_upper_arm", frames: [
                    Frame("0.0").Rotation("28.0", 0, "3.0"),
                    Frame("0.25", interpolation: "SMOOTH").Rotation(0, 0, "2.0"),
                    Frame("0.5", interpolation: "SMOOTH").Rotation("-28.0", 0, "3.0"),
                    Frame("0.75", interpolation: "SMOOTH").Rotation(0, 0, "2.0"),
                    Frame("1.0", interpolation: "SMOOTH").Rotation("28.0", 0, "3.0")
                ]),
                Track("left_forearm", frames: [
                    Frame("0.0").Rotation("12.0", 0, 0),
                    Frame("0.25", interpolation: "SMOOTH").Rotation("16.0", 0, 0),
                    Frame("0.5", interpolation: "SMOOTH").Rotation("22.0", 0, 0),
                    Frame("0.75", interpolation: "SMOOTH").Rotation("16.0", 0, 0),
                    Frame("1.0", interpolation: "SMOOTH").Rotation("12.0", 0, 0)
                ]),
                Track("right_forearm", frames: [
                    Frame("0.0").Rotation("22.0", 0, 0),
                    Frame("0.25", interpolation: "SMOOTH").Rotation("16.0", 0, 0),
                    Frame("0.5", interpolation: "SMOOTH").Rotation("12.0", 0, 0),
                    Frame("0.75", interpolation: "SMOOTH").Rotation("16.0", 0, 0),
                    Frame("1.0", interpolation: "SMOOTH").Rotation("22.0", 0, 0)
                ]),
                Track("left_thigh", frames: [
                    Frame("0.0").Rotation("30.0", 0, 0),
                    Frame("0.25", interpolation: "SMOOTH").Rotation(0, 0, 0),
                    Frame("0.5", interpolation: "SMOOTH").Rotation("-30.0", 0, 0),
                    Frame("0.75", interpolation: "SMOOTH").Rotation(0, 0, 0),
                    Frame("1.0", interpolation: "SMOOTH").Rotation("30.0", 0, 0)
                ]),
                Track("right_thigh", frames: [
                    Frame("0.0").Rotation("-30.0", 0, 0),
                    Frame("0.25", interpolation: "SMOOTH").Rotation(0, 0, 0),
                    Frame("0.5", interpolation: "SMOOTH").Rotation("30.0", 0, 0),
                    Frame("0.75", interpolation: "SMOOTH").Rotation(0, 0, 0),
                    Frame("1.0", interpolation: "SMOOTH").Rotation("-30.0", 0, 0)
                ]),
                Track("left_shin", frames: [
                    Frame("0.0").Rotation("5.0", 0, 0),
                    Frame("0.25", interpolation: "SMOOTH").Rotation("10.0", 0, 0),
                    Frame("0.5", interpolation: "SMOOTH").Rotation("35.0", 0, 0),
                    Frame("0.75", interpolation: "SMOOTH").Rotation("45.0", 0, 0),
                    Frame("1.0", interpolation: "SMOOTH").Rotation("5.0", 0, 0)
                ]),
                Track("right_shin", frames: [
                    Frame("0.0").Rotation("35.0", 0, 0),
                    Frame("0.25", interpolation: "SMOOTH").Rotation("45.0", 0, 0),
                    Frame("0.5", interpolation: "SMOOTH").Rotation("5.0", 0, 0),
                    Frame("0.75", interpolation: "SMOOTH").Rotation("10.0", 0, 0),
                    Frame("1.0", interpolation: "SMOOTH").Rotation("35.0", 0, 0)
                ])
            ]);
    }
}
