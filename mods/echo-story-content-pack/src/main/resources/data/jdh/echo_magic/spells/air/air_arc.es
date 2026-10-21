namespace EchoAirMagic.Spells;

class AirArcSpell : SpellResource
{
    private MagicSchoolId AirMagic = "echo_air_magic:air";
    private MagicDamageTypeId AirDamage = "echo_air_magic:air_magic";
    private MagicParticleStyleId AirCircle = "echo_air_magic:air_circle";

    private void Init()
    {
        Id("echo_air_magic:air_arc");
        DisplayName("Air Arc");
        School(AirMagic);
        Technique(ARC);
        Cooldown(Ticks(28));
        Cast(
            Origin(PLAYER_EYES),
            Direction(PLAYER_LOOK),
            Hand(MAIN_HAND),
            SwingHand(true)
        );
        Burst(
            count: 9,
            interval: Ticks(1),
            spread: Degrees(108)
        );
        Projectile(
            speed: BlocksPerTick(1.25),
            range: Blocks(11.0),
            radius: 0.38,
            gravity: 0.0,
            pierce: 0
        );
        Collision(blocks: STOP, entities: FIRST_VALID_TARGET);
        Targets(LIVING_EXCEPT_CASTER);
        FriendlyFire(false);
        Trail(
            particle: AirCircle,
            spacing: 0.13,
            size: 0.3,
            lifetime: Ticks(11),
            color: Color("#FFFFFF"),
            vanillaColor: Color("#A8D8FF"),
            alphaStart: 0.88,
            alphaEnd: 0.0
        );
        Core(particle: AirCircle, size: 0.2, color: Color("#FFFFFF"),
            vanillaColor: Color("#E8F7FF"));
        Hit(
            Damage(type: AirDamage, amount: 0.65),
            Knockback(strength: 1.35, direction: FORWARD),
            DeflectProjectiles(true)
        );
    }
}
