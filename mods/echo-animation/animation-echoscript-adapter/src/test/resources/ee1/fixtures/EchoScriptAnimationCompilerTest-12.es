namespace JDH.Animation;

class StandardProfile : AnimationProfileResource
{
    public override AnimationProfileDef Build()
    {
        return AnimationProfile("jdh:standard_humanoid",
            rig: "jdh:humanoid",
            graph: "jdh:humanoid",
            initialStates: [InitialState("body", state: "body/idle")],
            requiredChannels: [Channel("body")],
            render: "SEGMENTED_HUMANOID");
    }
}
