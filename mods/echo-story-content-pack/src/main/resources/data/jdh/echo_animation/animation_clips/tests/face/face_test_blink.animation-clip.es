namespace JDH.Animation.Tests.Face;

class FaceTestBlinkClip : AnimationClipResource
{
    public override AnimationClipDef Build()
    {
        return AnimationClip("jdh:face_test_blink", rig: "jdh:john_father",
            tracks: [
                Track("upper_eyelids", frames: [
                    Frame("0.0").Position(0, 0, 0),
                    Frame("0.28").Position(0, "0.035", 0),
                    Frame("0.52").Position(0, "0.035", 0),
                    Frame("1.0").Position(0, 0, 0)
                ]),
                Track("lower_eyelids", frames: [
                    Frame("0.0").Position(0, 0, 0),
                    Frame("0.28").Position(0, "-0.035", 0),
                    Frame("0.52").Position(0, "-0.035", 0),
                    Frame("1.0").Position(0, 0, 0)
                ])
            ]);
    }
}
