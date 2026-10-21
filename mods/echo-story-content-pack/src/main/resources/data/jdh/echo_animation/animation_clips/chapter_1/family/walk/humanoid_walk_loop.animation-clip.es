namespace JDH.Animation;

class HumanoidWalkLoopClip : AnimationClipResource
{
    public override AnimationClipDef Build()
    {
        return AnimationClip("jdh:humanoid_walk_loop", rig: "jdh:humanoid",
            schemaVersion: 2, loopSeam: "SMOOTH", authoredSpeedBlocksPerSecond: "1.35",
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
                    Frame("0.0", interpolation: "CUBIC").Rotation(0, 0, 0),
                    Frame("0.125", interpolation: "CUBIC").Rotation("16.3", 0, 0),
                    Frame("0.25", interpolation: "CUBIC").Rotation("23.0", 0, 0),
                    Frame("0.375", interpolation: "CUBIC").Rotation("16.3", 0, 0),
                    Frame("0.5", interpolation: "CUBIC").Rotation(0, 0, 0),
                    Frame("0.625", interpolation: "CUBIC").Rotation("-16.3", 0, 0),
                    Frame("0.75", interpolation: "CUBIC").Rotation("-23.0", 0, 0),
                    Frame("0.875", interpolation: "CUBIC").Rotation("-16.3", 0, 0),
                    Frame("1.0").Rotation(0, 0, 0)
                ]),
                Track("left_forearm", frames: [
                    Frame("0.0").Rotation(0, 0, 0),
                    Frame("1.0").Rotation(0, 0, 0)
                ]),
                Track("right_upper_arm", frames: [
                    Frame("0.0", interpolation: "CUBIC").Rotation(0, 0, 0),
                    Frame("0.125", interpolation: "CUBIC").Rotation("-16.3", 0, 0),
                    Frame("0.25", interpolation: "CUBIC").Rotation("-23.0", 0, 0),
                    Frame("0.375", interpolation: "CUBIC").Rotation("-16.3", 0, 0),
                    Frame("0.5", interpolation: "CUBIC").Rotation(0, 0, 0),
                    Frame("0.625", interpolation: "CUBIC").Rotation("16.3", 0, 0),
                    Frame("0.75", interpolation: "CUBIC").Rotation("23.0", 0, 0),
                    Frame("0.875", interpolation: "CUBIC").Rotation("16.3", 0, 0),
                    Frame("1.0").Rotation(0, 0, 0)
                ]),
                Track("right_forearm", frames: [
                    Frame("0.0").Rotation(0, 0, 0),
                    Frame("1.0").Rotation(0, 0, 0)
                ]),
                Track("left_thigh", frames: [
                    Frame("0.0", interpolation: "CUBIC").Rotation(0, 0, 0),
                    Frame("0.125", interpolation: "CUBIC").Rotation("-22.6", 0, 0),
                    Frame("0.25", interpolation: "CUBIC").Rotation("-32.0", 0, 0),
                    Frame("0.375", interpolation: "CUBIC").Rotation("-22.6", 0, 0),
                    Frame("0.5", interpolation: "CUBIC").Rotation(0, 0, 0),
                    Frame("0.625", interpolation: "CUBIC").Rotation("22.6", 0, 0),
                    Frame("0.75", interpolation: "CUBIC").Rotation("32.0", 0, 0),
                    Frame("0.875", interpolation: "CUBIC").Rotation("22.6", 0, 0),
                    Frame("1.0").Rotation(0, 0, 0)
                ]),
                Track("left_shin", frames: [
                    Frame("0.0").Rotation(0, 0, 0),
                    Frame("1.0").Rotation(0, 0, 0)
                ]),
                Track("right_thigh", frames: [
                    Frame("0.0", interpolation: "CUBIC").Rotation(0, 0, 0),
                    Frame("0.125", interpolation: "CUBIC").Rotation("22.6", 0, 0),
                    Frame("0.25", interpolation: "CUBIC").Rotation("32.0", 0, 0),
                    Frame("0.375", interpolation: "CUBIC").Rotation("22.6", 0, 0),
                    Frame("0.5", interpolation: "CUBIC").Rotation(0, 0, 0),
                    Frame("0.625", interpolation: "CUBIC").Rotation("-22.6", 0, 0),
                    Frame("0.75", interpolation: "CUBIC").Rotation("-32.0", 0, 0),
                    Frame("0.875", interpolation: "CUBIC").Rotation("-22.6", 0, 0),
                    Frame("1.0").Rotation(0, 0, 0)
                ]),
                Track("right_shin", frames: [
                    Frame("0.0").Rotation(0, 0, 0),
                    Frame("1.0").Rotation(0, 0, 0)
                ])
            ]);
    }
}
