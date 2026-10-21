namespace JDH.Animation;

class HumanoidRig : AnimationRigResource
{
    public override AnimationRigDef Build()
    {
        return AnimationRig("jdh:humanoid", kind: "SEGMENTED_HUMANOID", skin: "CLASSIC");
    }
}
