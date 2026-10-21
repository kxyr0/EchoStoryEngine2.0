namespace JDH.Animation;

class NeutralPose : FacePoseResource
{
    public override FacePoseDef Build()
    {
        // Русский комментарий должен пережить UTF-8 parsing.
        return FacePose("jdh:john_father/neutral", rig: "jdh:john_father",
            bones: [BoneDelta("face_root", y: 10)],
            controls: [ControlDelta("eye_closure", value: 0)]);
    }
}
