namespace JDH.Animation;

class WaveLoopClip : AnimationClipResource
{
    public override AnimationClipDef Build()
    {
        return AnimationClip("jdh:wave_loop", rig: "jdh:humanoid",
            tracks: [
                Track("right_shoulder", frames: [
                    Frame("0.0").Rotation(0, 0, "-24.0"),
                    Frame("1.0").Rotation(0, 0, "-24.0")
                ]),
                Track("right_upper_arm", frames: [
                    Frame("0.0").Rotation("-72.0", 0, 0),
                    Frame("1.0").Rotation("-72.0", 0, 0)
                ]),
                Track("right_forearm", frames: [
                    Frame("0.0").Rotation(0, 0, "36.0"),
                    Frame("0.25").Rotation(0, 0, "-28.0"),
                    Frame("0.5").Rotation(0, 0, "36.0"),
                    Frame("0.75").Rotation(0, 0, "-28.0"),
                    Frame("1.0").Rotation(0, 0, "36.0")
                ])
            ]);
    }
}
