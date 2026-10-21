namespace JDH.NpcAnimation.Chapter1.Family;

class JohnFatherNpcAnimationBinding : NpcAnimationBindingResource
{
    public override NpcAnimationBindingDef Build()
    {
        return NpcAnimationBinding("jdh:john_father_standard_humanoid",
            npc: "jdh:john_father",
            profile: "jdh:standard_humanoid"
        );
    }
}
