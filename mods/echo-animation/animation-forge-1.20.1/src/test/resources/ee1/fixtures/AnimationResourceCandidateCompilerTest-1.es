namespace JDH.Animation;

class Rig : AnimationRigResource
{
    public override AnimationRigDef Build()
    {
        return AnimationRig("jdh:humanoid", skin: "CLASSIC");
    }
}
