namespace JDH.Animation;

class BadRig : FaceRigResource
{
    public override FaceRigDef Build()
    {
        return FaceRig("jdh:bad", unexpected: 1, bones: [Bone("face_root")]);
    }
}
