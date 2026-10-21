namespace JDH.Npc.AnimationBindings;

class MollyBackpackAnimations : NpcBackpackAnimationResource
{
    private void Init()
    {
        Npc("jdh:molly");
        Equip(
            animation: "jdh:molly/equip_backpack",
            take: Marker("echo_npc_sophisticated_backpacks:take_from_world"),
            hand: Marker("echo_npc_sophisticated_backpacks:attach_right_hand"),
            wear: Marker("echo_npc_sophisticated_backpacks:attach_back")
        );
        Unequip(
            animation: "jdh:molly/unequip_backpack",
            remove: Marker("echo_npc_sophisticated_backpacks:detach_from_back"),
            hand: Marker("echo_npc_sophisticated_backpacks:attach_right_hand"),
            place: Marker("echo_npc_sophisticated_backpacks:place_in_world")
        );
    }
}
