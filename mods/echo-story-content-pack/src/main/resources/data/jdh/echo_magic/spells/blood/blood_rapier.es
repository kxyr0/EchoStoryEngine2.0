namespace EchoBloodMagic.Spells;

class BloodRapierSpell : SpellResource
{
    private void Init()
    {
        Id("echo_blood_magic:blood_rapier");
        DisplayName("Blood Rapier");
        School("echo_blood_magic:blood");
        Technique(WEAPON);
        Cooldown(Ticks(300));

        Cast(
            Origin(PLAYER_CENTER),
            Direction(PLAYER_LOOK),
            Hand(MAIN_HAND),
            SwingHand(true)
        );
        Burst(count: 1, interval: Ticks(1));
        Projectile(
            speed: BlocksPerTick(0.01),
            range: Blocks(0.1),
            radius: 0.01,
            gravity: 0.0,
            pierce: 0
        );
        Collision(blocks: STOP, entities: FIRST_VALID_TARGET);
        Targets(LIVING_EXCEPT_CASTER);
        FriendlyFire(false);
        Trail(
            particle: "echo_blood_magic:blood_circle",
            spacing: 0.1,
            size: 0.08,
            lifetime: Ticks(2),
            color: Color("#8B0000"),
            vanillaColor: Color("#8B0000"),
            alphaStart: 0.1,
            alphaEnd: 0.0
        );
        Core(
            particle: "echo_blood_magic:blood_circle",
            size: 0.05,
            color: Color("#FF3030"),
            vanillaColor: Color("#FF3030")
        );
        Hit(Damage(type: "echo_blood_magic:blood_magic", amount: 0.01));
    }
}
