namespace JDH.Animation.Tests.Face;

class FaceTestAngryBrowsClip : AnimationClipResource
{
    public override AnimationClipDef Build()
    {
        return AnimationClip("jdh:face_test_angry_brows", rig: "jdh:john_father",
            tracks: [
                Track("brows_left", frames: [
                    Frame("0.0").Position(0, 0, 0),
                    Frame("0.5").Position("0.012", "0.012", 0),
                    Frame("1.0").Position("0.012", "0.012", 0)
                ]),
                Track("brows_right", frames: [
                    Frame("0.0").Position(0, 0, 0),
                    Frame("0.5").Position("-0.012", "0.012", 0),
                    Frame("1.0").Position("-0.012", "0.012", 0)
                ])
            ]);
    }
}
