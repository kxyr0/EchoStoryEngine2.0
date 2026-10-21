namespace JDH.Animation;

class HumanoidWalkExitClip : AnimationClipResource
{
    public override AnimationClipDef Build()
    {
        return AnimationClip("jdh:humanoid_walk_exit", rig: "jdh:humanoid",
            tracks: [
                Track("body", frames: [
                    Frame("0.0").Position(0, "12.0", 0).Rotation(0, 0, 0),
                    Frame("1.0").Position(0, "12.0", 0).Rotation(0, 0, 0)
                ]),
                Track("head", frames: [
                    Frame("0.0").Rotation(0, 0, 0),
                    Frame("1.0").Rotation(0, 0, 0)
                ]),
                Track("left_shoulder", frames: [
                    Frame("0.0").Rotation(0, 0, 0),
                    Frame("1.0").Rotation(0, 0, 0)
                ]),
                Track("right_shoulder", frames: [
                    Frame("0.0").Rotation(0, 0, 0),
                    Frame("1.0").Rotation(0, 0, 0)
                ]),
                Track("left_upper_arm", frames: [
                    Frame("0.0").Rotation(0, 0, 0),
                    Frame("1.0").Rotation(0, 0, 0)
                ]),
                Track("left_forearm", frames: [
                    Frame("0.0").Rotation(0, 0, 0),
                    Frame("1.0").Rotation(0, 0, 0)
                ]),
                Track("right_upper_arm", frames: [
                    Frame("0.0").Rotation(0, 0, 0),
                    Frame("1.0").Rotation(0, 0, 0)
                ]),
                Track("right_forearm", frames: [
                    Frame("0.0").Rotation(0, 0, 0),
                    Frame("1.0").Rotation(0, 0, 0)
                ]),
                Track("left_thigh", frames: [
                    Frame("0.0").Rotation(0, 0, 0),
                    Frame("1.0").Rotation(0, 0, 0)
                ]),
                Track("left_shin", frames: [
                    Frame("0.0").Rotation(0, 0, 0),
                    Frame("1.0").Rotation(0, 0, 0)
                ]),
                Track("right_thigh", frames: [
                    Frame("0.0").Rotation(0, 0, 0),
                    Frame("1.0").Rotation(0, 0, 0)
                ]),
                Track("right_shin", frames: [
                    Frame("0.0").Rotation(0, 0, 0),
                    Frame("1.0").Rotation(0, 0, 0)
                ])
            ]);
    }
}
