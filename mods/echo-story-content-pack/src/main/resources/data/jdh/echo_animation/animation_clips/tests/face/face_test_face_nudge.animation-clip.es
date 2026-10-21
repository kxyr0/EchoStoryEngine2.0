namespace JDH.Animation.Tests.Face;

class FaceTestFaceNudgeClip : AnimationClipResource
{
    public override AnimationClipDef Build()
    {
        return AnimationClip("jdh:face_test_face_nudge", rig: "jdh:john_father",
            tracks: [
                Track("face_root", frames: [
                    Frame("0.0").Position(0, 0, 0),
                    Frame("0.25").Position("0.025", "0.015", 0),
                    Frame("0.5").Position("-0.025", "-0.015", 0),
                    Frame("0.75").Position("0.015", 0, 0),
                    Frame("1.0").Position(0, 0, 0)
                ])
            ]);
    }
}
