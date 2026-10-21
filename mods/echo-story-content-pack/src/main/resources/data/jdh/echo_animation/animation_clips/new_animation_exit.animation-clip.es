namespace JDH.Animation;

class NewAnimationExitClip : AnimationClipResource
{
    public override AnimationClipDef Build()
    {
        return AnimationClip("jdh:new_animation_exit",
            rig: "jdh:humanoid",
            schemaVersion: 2,
            loopSeam: "HOLD",
            tracks: [
                Track("left_shoulder", frames: [
                    Frame("0.0", interpolation: "SMOOTH").Position("3.971986", "9.906662", "-0.025391").Rotation("0.0", "0.0", "0.0").Scale("1.0", "1.0", "1.0"),
                    Frame("1.0").Position("4.0", "10.0", "0.0").Rotation("0.0", "0.0", "0.0").Scale("1.0", "1.0", "1.0")
                ]),
                Track("right_upper_arm", frames: [
                    Frame("0.0", interpolation: "SMOOTH").Position("0.0", "0.0", "0.0").Rotation("0.255973", "-0.205535", "-0.000463").Scale("1.0", "1.0", "1.0"),
                    Frame("1.0").Position("0.0", "0.0", "0.0").Rotation("0.0", "0.0", "0.0").Scale("1.0", "1.0", "1.0")
                ])
            ]);
    }
}
