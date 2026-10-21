namespace JDH.Animation.Chapter1.Family;

class StandardHumanoidAnimationProfile : AnimationProfileResource
{
    public override AnimationProfileDef Build()
    {
        return AnimationProfile("jdh:standard_humanoid",
            rig: "jdh:humanoid",
            graph: "jdh:humanoid",
            render: "SEGMENTED_HUMANOID",
            initialStates: [
                InitialState("body", state: "body/idle")
            ],
            requiredChannels: [
                Channel("body")
            ],
            metadata: [
                Meta("authoring_note", value: "Default segmented humanoid profile for John Father.")
            ]
        );
    }
}
