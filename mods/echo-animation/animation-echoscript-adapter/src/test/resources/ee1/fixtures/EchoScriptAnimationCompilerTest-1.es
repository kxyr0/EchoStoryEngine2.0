namespace JDH.Animation;

class JohnFatherRig : FaceRigResource
{
    public override FaceRigDef Build()
    {
        return FaceRig("jdh:john_father",
            bones: [Bone("face_root")],
            layers: [Layer("glasses", bone: "face_root", texture: "jdh:face/glasses",
                    role: "GLASSES", order: 20, x: 0, y: 0, w: 8, h: 8,
                    placeX: 120, placeY: 250, placeZ: "0.63")],
            controls: [BlinkControl("eye_closure", upperTravel: 1000, lowerTravel: 1000)]
        );
    }
}
