namespace EchoBackgroundNpc.Animation;

class AmbientTalkLoop : AnimationClipResource
{
    public override AnimationClipDef Build()
    {
        return AnimationClip("echo_background_npc:talk_loop", rig: "echo_animation:segmented_humanoid",
            schemaVersion: 2, loopSeam: "SMOOTH",
            tracks: [
                Track("right_upper_arm", frames: [
                    Frame("0.0", interpolation: "CUBIC").Rotation("-18.0", 0, "5.0"),
                    Frame("0.5", interpolation: "CUBIC").Rotation("-34.0", 0, "-8.0"),
                    Frame("1.0").Rotation("-18.0", 0, "5.0")
                ]),
                Track("right_forearm", frames: [
                    Frame("0.0", interpolation: "CUBIC").Rotation("-16.0", 0, 0),
                    Frame("0.5", interpolation: "CUBIC").Rotation("-42.0", 0, 0),
                    Frame("1.0").Rotation("-16.0", 0, 0)
                ]),
                Track("head", frames: [
                    Frame("0.0", interpolation: "CUBIC").Rotation("-2.0", "-3.0", 0),
                    Frame("0.5", interpolation: "CUBIC").Rotation("2.0", "3.0", 0),
                    Frame("1.0").Rotation("-2.0", "-3.0", 0)
                ])
            ]);
    }
}
