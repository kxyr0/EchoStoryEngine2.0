namespace EchoAnimation.Builtin;

class SegmentedHumanoidRig : AnimationRigResource
{
    public override AnimationRigDef Build()
    {
        return AnimationRig("echo_animation:segmented_humanoid",
            kind: "SEGMENTED_HUMANOID", skin: "CLASSIC");
    }
}
