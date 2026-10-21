namespace JDH.Animation;

class Clip : AnimationClipResource
{
    public override AnimationClipDef Build()
    {
        return AnimationClip("%s", rig: "jdh:humanoid",
            tracks: [Track("%s",
                    frames: [Frame("0.0", rotZ: "0.0"),
                             Frame("1.0", rotZ: "0.0")])]);
    }
}
