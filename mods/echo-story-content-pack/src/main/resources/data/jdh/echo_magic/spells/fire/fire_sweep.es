namespace EchoFireMagic.Spells;

class FireSweepSpell : SpellResource
{
    private MagicSchoolId FireMagic = "echo_fire_magic:fire";
    private MagicDamageTypeId FireDamage = "echo_fire_magic:fire_magic";
    private MagicParticleStyleId FireFlame = "echo_fire_magic:fire_flame";

    private void Init()
    {
        Id("echo_fire_magic:fire_sweep");
        DisplayName("Fire Sweep");
        School(FireMagic);
        Technique(ARC);
        Cooldown(Ticks(34));
        Cast(
            Origin(PLAYER_EYES),
            Direction(PLAYER_LOOK),
            Hand(MAIN_HAND),
            SwingHand(true)
        );
        Burst(
            count: 7,
            interval: Ticks(1),
            spread: Degrees(82)
        );
        Projectile(
            speed: BlocksPerTick(0.95),
            range: Blocks(8.0),
            radius: 0.34,
            gravity: 0.0,
            pierce: 0
        );
        Collision(blocks: STOP, entities: FIRST_VALID_TARGET);
        Targets(LIVING_EXCEPT_CASTER);
        FriendlyFire(false);
        Trail(
            particle: FireFlame,
            spacing: 0.13,
            size: 0.31,
            lifetime: Ticks(12),
            color: Color("#FFFFFF"),
            vanillaColor: Color("#FF5A18"),
            alphaStart: 0.94,
            alphaEnd: 0.0
        );
        Core(particle: FireFlame, size: 0.22, color: Color("#FFF2B2"),
            vanillaColor: Color("#FFD35A"));
        Hit(
            Damage(type: FireDamage, amount: 1.25),
            Ignite(duration: Ticks(100))
        );
    }
}
