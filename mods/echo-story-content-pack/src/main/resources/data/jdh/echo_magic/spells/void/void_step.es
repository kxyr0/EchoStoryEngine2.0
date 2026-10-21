namespace EchoVoidMagic.Spells;

class VoidStepSpell : SpellResource
{
    private void Init()
    {
        Id("echo_void_magic:void_step");
        DisplayName("Void Step");
        School("echo_void_magic:void");
        Technique(POINT);
        Cooldown(Ticks(30));

        Cast(
            Origin(PLAYER_CENTER),
            Direction(PLAYER_HORIZONTAL_LOOK),
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
            particle: "echo_void_magic:void_circle",
            spacing: 0.1,
            size: 0.08,
            lifetime: Ticks(2),
            color: Color("#0A5E22"),
            vanillaColor: Color("#14FF42"),
            alphaStart: 0.1,
            alphaEnd: 0.0
        );
        Core(
            particle: "echo_void_magic:void_circle",
            size: 0.05,
            color: Color("#32FF58"),
            vanillaColor: Color("#32FF58")
        );
        Hit(Damage(type: "echo_void_magic:void_magic", amount: 0.01));
    }
}
