namespace JDH.Animation.Tests.Face.Blink;

class FaceTestBlinkClosedClip : AnimationClipResource
{
    public override AnimationClipDef Build()
    {
        return AnimationClip("jdh:face_test_blink_closed", rig: "jdh:john_father",
            tracks: [
                Track("upper_eyelids", frames: [
                    Frame("0.0").Position(0, 0, 0),
                    Frame("1.0").Position(0, "0.035", 0)
                ]),
                Track("lower_eyelids", frames: [
                    Frame("0.0").Position(0, 0, 0),
                    Frame("1.0").Position(0, "-0.035", 0)
                ])
            ]);
    }
}
