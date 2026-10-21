namespace EchoFireMagic.Spells;

class BlacksmithHandSpell : SpellResource
{
    private void Init()
    {
        Id("echo_fire_magic:blacksmith_hand");
        DisplayName("Blacksmith's Hand");
        School("echo_fire_magic:fire");
        Technique(POINT);
        Cooldown(Ticks(120));

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
            particle: "echo_fire_magic:fire_flame",
            spacing: 0.1,
            size: 0.08,
            lifetime: Ticks(2),
            color: Color("#FF8A22"),
            vanillaColor: Color("#FF8A22"),
            alphaStart: 0.1,
            alphaEnd: 0.0
        );
        Core(
            particle: "echo_fire_magic:fire_flame",
            size: 0.05,
            color: Color("#FFD36A"),
            vanillaColor: Color("#FFD36A")
        );
        Hit(Damage(type: "echo_fire_magic:fire_magic", amount: 0.01));
    }
}
