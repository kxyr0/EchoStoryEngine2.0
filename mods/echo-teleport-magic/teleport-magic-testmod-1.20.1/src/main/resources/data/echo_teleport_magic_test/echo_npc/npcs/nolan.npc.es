namespace Echo.Teleport.Test;
class NolanNpc : NpcResource {
    public override NpcResourceDef Build() {
        return Npc(id: "echo_teleport_magic_test:nolan", display: "ETM Nolan");
    }
}
