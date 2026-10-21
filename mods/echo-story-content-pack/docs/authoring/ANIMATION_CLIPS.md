# Animation Clips

A2 body clips use normalized keyframe time and live under:

```text
data/<namespace>/echo_animation/animation_clips/**/*.animation-clip.es
```

Graph state owns playback duration and mode. Clip keyframes only describe how
bones look at progress `0.0` through `1.0`.

## Idle Clip

```csharp
namespace JDH.Animation;

class HumanoidIdleClip : AnimationClipResource
{
    public override AnimationClipDef Build()
    {
        return AnimationClip("jdh:humanoid_idle", rig: "jdh:humanoid", tracks: []);
    }
}
```

## Wave Clip

```csharp
namespace JDH.Animation;

class HumanoidWaveClip : AnimationClipResource
{
    public override AnimationClipDef Build()
    {
        return AnimationClip("jdh:humanoid_test_wave", rig: "jdh:humanoid",
            tracks: [
                Track("right_shoulder", frames: [
                    Frame("0.0", rotZ: "0.0"),
                    Frame("0.5", rotZ: "-20.0"),
                    Frame("1.0", rotZ: "0.0")
                ]),
                Track("right_upper_arm", frames: [
                    Frame("0.0", rotX: "0.0"),
                    Frame("0.5", rotX: "-55.0"),
                    Frame("1.0", rotX: "0.0")
                ]),
                Track("right_forearm", frames: [
                    Frame("0.0", rotX: "0.0"),
                    Frame("0.5", rotX: "-65.0"),
                    Frame("1.0", rotX: "0.0")
                ])
            ]);
    }
}
```

Use string decimals for negative numbers. Integer numeric literals are
interpreted as milli-units, so `500` means `0.5`.
