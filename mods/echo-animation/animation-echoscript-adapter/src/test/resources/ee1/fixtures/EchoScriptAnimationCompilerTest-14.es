namespace JDH.Animation;

class BadAnimation : AnimationResource
{
    public override AnimationDef Build()
    {
        return Animation("jdh:bad", unexpected: 1);
    }
}
