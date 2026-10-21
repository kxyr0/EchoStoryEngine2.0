namespace JDH.Animation.Tests.Face;

class FaceTestNeutralClip : AnimationClipResource
{
    public override AnimationClipDef Build()
    {
        return AnimationClip("jdh:face_test_neutral", rig: "jdh:john_father",
            tracks: [
                Track("face_root", frames: [
                    Frame("0.0").Position(0, 0, 0).Rotation(0, 0, 0)
                ])
            ]);
    }
}
