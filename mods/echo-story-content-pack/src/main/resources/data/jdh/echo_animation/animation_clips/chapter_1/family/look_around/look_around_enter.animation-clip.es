namespace JDH.Animation;

class LookAroundEnterClip : AnimationClipResource
{
    public override AnimationClipDef Build()
    {
        return AnimationClip("jdh:look_around_enter", rig: "jdh:humanoid",
            tracks: [
                Track("head", frames: [Frame("0.0").Rotation(0, "0.0", 0), Frame("1.0").Rotation(0, "0.0", 0)])
            ]);
    }
}

