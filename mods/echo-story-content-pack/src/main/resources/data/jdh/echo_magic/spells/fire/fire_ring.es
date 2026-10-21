namespace EchoFireMagic.Spells;

class FireRingSpell : SpellResource
{
    private MagicSchoolId FireMagic = "echo_fire_magic:fire";
    private MagicDamageTypeId FireDamage = "echo_fire_magic:fire_magic";
    private MagicParticleStyleId FireFlame = "echo_fire_magic:fire_flame";

    private void Init()
    {
        Id("echo_fire_magic:fire_ring");
        DisplayName("Fire Ring");
        School(FireMagic);
        Technique(CIRCLE);
        Cooldown(Ticks(50));
        Cast(
            Origin(PLAYER_CENTER),
            Direction(PLAYER_HORIZONTAL_LOOK),
            Hand(MAIN_HAND),
            SwingHand(true)
        );
        Burst(
            count: 16,
            interval: Ticks(0),
            spread: Degrees(337.5)
        );
        Projectile(
            speed: BlocksPerTick(0.58),
            range: Blocks(5.0),
            radius: 0.32,
            gravity: 0.0,
            pierce: 0
        );
        Collision(blocks: STOP, entities: FIRST_VALID_TARGET);
        Targets(LIVING_EXCEPT_CASTER);
        FriendlyFire(false);
        Trail(
            particle: FireFlame,
            spacing: 0.12,
            size: 0.3,
            lifetime: Ticks(13),
            color: Color("#FFFFFF"),
            vanillaColor: Color("#FF5A18"),
            alphaStart: 0.94,
            alphaEnd: 0.0
        );
        Core(particle: FireFlame, size: 0.2, color: Color("#FFF2B2"),
            vanillaColor: Color("#FFD35A"));
        Hit(
            Damage(type: FireDamage, amount: 0.6),
            Ignite(duration: Ticks(120))
        );
    }
}
