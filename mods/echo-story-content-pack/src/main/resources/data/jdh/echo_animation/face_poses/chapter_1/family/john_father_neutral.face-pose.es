namespace JDH.Animation.Chapter1.Family;

class JohnFatherNeutralFacePose : FacePoseResource
{
    public override FacePoseDef Build()
    {
        return FacePose("jdh:john_father/neutral", rig: "jdh:john_father",
            bones: [
                BoneDelta("face_root")
            ],
            controls: [
                ControlDelta("eye_closure", value: 0)
            ]
        );
    }
}

