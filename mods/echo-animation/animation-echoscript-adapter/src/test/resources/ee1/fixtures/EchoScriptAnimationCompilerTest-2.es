namespace JDH.Animation;

class JohnFatherRig : FaceRigResource
{
    public override FaceRigDef Build()
    {
        return FaceRig("jdh:john_father",
            bones: [
                Bone("face_root"),
                Bone("left_eye_root", parent: "face_root"),
                Bone("left_sclera", parent: "left_eye_root"),
                Bone("left_pupil", parent: "left_sclera"),
                Bone("right_eye_root", parent: "face_root"),
                Bone("right_sclera", parent: "right_eye_root"),
                Bone("right_pupil", parent: "right_sclera")
            ],
            layers: [
                Layer("sclera_left", bone: "left_sclera",
                    texture: "jdh:face/sclera_left", role: "DECORATION",
                    order: 20, x: 0, y: 0, w: 2, h: 2),
                Layer("pupil_left", bone: "left_pupil",
                    texture: "jdh:face/pupil_left", role: "PUPIL",
                    order: 30, x: 0, y: 0, w: 1, h: 2),
                Layer("sclera_right", bone: "right_sclera",
                    texture: "jdh:face/sclera_right", role: "DECORATION",
                    order: 21, x: 0, y: 0, w: 2, h: 2),
                Layer("pupil_right", bone: "right_pupil",
                    texture: "jdh:face/pupil_right", role: "PUPIL",
                    order: 31, x: 0, y: 0, w: 1, h: 2)
            ]
        );
    }
}
