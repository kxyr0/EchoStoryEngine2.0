namespace EchoNpc.Sample;

class SimpleNpc : NpcResource
{
    public override NpcResourceDef Build()
    {
        return Npc(
            id: "sample:simple",
            display: "Житель"
        );
    }
}
