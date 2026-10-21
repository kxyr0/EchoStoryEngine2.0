namespace JDH.Animation;

class WaveClip : AnimationClipResource
{
    public override AnimationClipDef Build()
    {
        return AnimationClip("jdh:humanoid_test_wave", rig: "jdh:humanoid",
            schemaVersion: 2, loopSeam: "SMOOTH", authoredSpeedBlocksPerSecond: "1.2",
            tracks: [Track("right_forearm",
                    frames: [Frame("0.0", rotZ: 0),
                             Frame("0.5", rotZ: "-45.0", interpolation: "SMOOTH"),
                             Frame("1.0", rotZ: 0)])]);
    }
}
