namespace EchoAirMagic.Spells;

class AirRingSpell : SpellResource
{
    private MagicSchoolId AirMagic = "echo_air_magic:air";
    private MagicDamageTypeId AirDamage = "echo_air_magic:air_magic";
    private MagicParticleStyleId AirCircle = "echo_air_magic:air_circle";

    private void Init()
    {
        Id("echo_air_magic:air_ring");
        DisplayName("Air Ring");
        School(AirMagic);
        Technique(CIRCLE);
        Cooldown(Ticks(42));
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
            speed: BlocksPerTick(0.72),
            range: Blocks(6.0),
            radius: 0.34,
            gravity: 0.0,
            pierce: 0
        );
        Collision(blocks: STOP, entities: FIRST_VALID_TARGET);
        Targets(LIVING_EXCEPT_CASTER);
        FriendlyFire(false);
        Trail(
            particle: AirCircle,
            spacing: 0.12,
            size: 0.29,
            lifetime: Ticks(12),
            color: Color("#FFFFFF"),
            vanillaColor: Color("#A8D8FF"),
            alphaStart: 0.9,
            alphaEnd: 0.0
        );
        Core(particle: AirCircle, size: 0.19, color: Color("#FFFFFF"),
            vanillaColor: Color("#E8F7FF"));
        Hit(
            Damage(type: AirDamage, amount: 0.4),
            Knockback(strength: 1.3, direction: FORWARD),
            DeflectProjectiles(true)
        );
    }
}
