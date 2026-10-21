namespace JDH.Animation.Tests.Face;

class FaceTestLookLeftClip : AnimationClipResource
{
    public override AnimationClipDef Build()
    {
        return AnimationClip("jdh:face_test_look_left", rig: "jdh:john_father",
            tracks: [
                Track("left_pupil", frames: [
                    Frame("0.0").Position(0, 0, 0),
                    Frame("0.5").Position("-0.025", 0, 0),
                    Frame("1.0").Position("-0.025", 0, 0)
                ]),
                Track("right_pupil", frames: [
                    Frame("0.0").Position(0, 0, 0),
                    Frame("0.5").Position("-0.025", 0, 0),
                    Frame("1.0").Position("-0.025", 0, 0)
                ])
            ]);
    }
}
