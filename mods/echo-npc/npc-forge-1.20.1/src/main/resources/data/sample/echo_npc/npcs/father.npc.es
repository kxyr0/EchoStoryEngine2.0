namespace EchoNpc.Sample;

class FatherNpc : NpcResource
{
    public override NpcResourceDef Build()
    {
        return Npc(
            id: "sample:father",
            display: "Отец",
            skin: Skin("sample:father")
        );
    }
}
