namespace JDH.Animation.Tests.Face;

class FaceTestFullFaceHoldClip : AnimationClipResource
{
    public override AnimationClipDef Build()
    {
        return AnimationClip("jdh:face_test_full_face_hold", rig: "jdh:john_father",
            tracks: [
                Track("brows_left", frames: [
                    Frame("0.0").Position(0, 0, 0).Rotation(0, 0, 0),
                    Frame("0.5").Position("0.004", "-0.010", 0).Rotation(0, 0, "-4.0"),
                    Frame("1.0").Position("0.004", "-0.010", 0).Rotation(0, 0, "-4.0")
                ]),
                Track("brows_right", frames: [
                    Frame("0.0").Position(0, 0, 0).Rotation(0, 0, 0),
                    Frame("0.5").Position("-0.004", "-0.010", 0).Rotation(0, 0, "4.0"),
                    Frame("1.0").Position("-0.004", "-0.010", 0).Rotation(0, 0, "4.0")
                ]),
                Track("upper_eyelids", frames: [
                    Frame("0.0").Position(0, 0, 0),
                    Frame("0.5").Position(0, "0.020", 0),
                    Frame("1.0").Position(0, "0.020", 0)
                ]),
                Track("lower_eyelids", frames: [
                    Frame("0.0").Position(0, 0, 0),
                    Frame("0.5").Position(0, "-0.020", 0),
                    Frame("1.0").Position(0, "-0.020", 0)
                ]),
                Track("left_pupil", frames: [
                    Frame("0.0").Position(0, 0, 0),
                    Frame("0.5").Position("-0.006", 0, 0),
                    Frame("1.0").Position("-0.006", 0, 0)
                ]),
                Track("right_pupil", frames: [
                    Frame("0.0").Position(0, 0, 0),
                    Frame("0.5").Position("0.006", 0, 0),
                    Frame("1.0").Position("0.006", 0, 0)
                ])
            ]);
    }
}
