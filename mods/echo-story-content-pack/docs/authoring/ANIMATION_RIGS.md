# Animation Rigs

A2 body rigs use Echo Animation resources under:

```text
data/<namespace>/echo_animation/animation_rigs/**/*.animation-rig.es
```

The runtime discovers these through the Minecraft resource system. During
external authoring the watched workspace is `<gameDir>/story/content-pack/`;
`jdh` is just the namespace directory inside that workspace.

## Segmented Humanoid

This is a complete working A2 rig resource:

```csharp
namespace JDH.Animation;

class HumanoidRig : AnimationRigResource
{
    public override AnimationRigDef Build()
    {
        return AnimationRig("jdh:humanoid", kind: "SEGMENTED_HUMANOID", skin: "CLASSIC");
    }
}
```

For `SEGMENTED_HUMANOID`, omitted `bones` compile to the canonical A2 hierarchy:

```text
root -> body -> head
body -> left_shoulder -> left_upper_arm -> left_forearm
body -> right_shoulder -> right_upper_arm -> right_forearm
root -> left_thigh -> left_shin
root -> right_thigh -> right_shin
```

No hand, foot, mouth, jaw, pelvis, spine, neck, finger, or toe bones exist in
this rig. Default sockets are `main_hand` on `right_forearm` and `off_hand` on
`left_forearm`.

Use `skin: "SLIM"` for Alex-width arms.
