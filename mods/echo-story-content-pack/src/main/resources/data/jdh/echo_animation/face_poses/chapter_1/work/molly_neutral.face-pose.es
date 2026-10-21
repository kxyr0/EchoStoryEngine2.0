namespace JDH.Animation.Chapter1.Work;

class MollyNeutralFacePose : FacePoseResource
{
    public override FacePoseDef Build()
    {
        return FacePose("jdh:molly/neutral", rig: "jdh:molly",
            bones: [
                BoneDelta("face_root")
            ],
            controls: [
                ControlDelta("eye_closure", value: 0)
            ]
        );
    }
}
