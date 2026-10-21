namespace JDH.Animation;

class KneeBendEnterClip : AnimationClipResource
{
    public override AnimationClipDef Build()
    {
        return AnimationClip("jdh:knee_bend_enter", rig: "jdh:humanoid",
            tracks: [
                Track("body", frames: [
                    Frame("0.0").Position("0.0", "12.0", "0.0"),
                    Frame("1.0").Position("0.0", "12.0", "0.0")
                ]),
                Track("left_thigh", frames: [Frame("0.0").Rotation("0.0", 0, 0), Frame("1.0").Rotation("0.0", 0, 0)]),
                Track("left_shin", frames: [Frame("0.0").Rotation("0.0", 0, 0), Frame("1.0").Rotation("0.0", 0, 0)]),
                Track("right_thigh", frames: [Frame("0.0").Rotation("0.0", 0, 0), Frame("1.0").Rotation("0.0", 0, 0)]),
                Track("right_shin", frames: [Frame("0.0").Rotation("0.0", 0, 0), Frame("1.0").Rotation("0.0", 0, 0)])
            ]);
    }
}

