namespace JDH.Acceptance;

class ContentPackFatherNpc : NpcResource
{
    public override NpcResourceDef Build()
    {
        return Npc(
            id: "jdh:content_pack_father",
            display: "Отец",
            skin: Skin("jdh:content_pack_father")
        );
    }
}

