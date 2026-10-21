namespace JDH.Animation;

class LookAroundMainClip : AnimationClipResource
{
    public override AnimationClipDef Build()
    {
        return AnimationClip("jdh:look_around_main", rig: "jdh:humanoid",
            tracks: [
                Track("head", frames: [
                    Frame("0.0").Rotation(0, "0.0", 0),
                    Frame("0.25").Rotation(0, "32.0", 0),
                    Frame("0.55").Rotation(0, "-32.0", 0),
                    Frame("0.8").Rotation(0, "18.0", 0),
                    Frame("1.0").Rotation(0, "0.0", 0)
                ])
            ]);
    }
}

