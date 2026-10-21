namespace EchoAirMagic.Spells;

class AirBoltSpell : SpellResource
{
    private SpellId AirBolt =
        "echo_air_magic:air_bolt";

    private MagicSchoolId AirMagic =
        "echo_air_magic:air";

    private MagicDamageTypeId AirDamage =
        "echo_air_magic:air_magic";

    private MagicParticleStyleId AirCircle =
        "echo_air_magic:air_circle";

    private double DamageAmount = 3.0;
    private double Speed = 1.65;
    private double Range = 24.0;
    private double HitRadius = 0.22;

    private void Init()
    {
        Id(AirBolt);
        DisplayName("Air Bolt");
        School(AirMagic);
        Technique(LINE);

        Cooldown(Ticks(16));

        Cast(
            Origin(PLAYER_EYES),
            Direction(PLAYER_LOOK),
            Hand(MAIN_HAND),
            SwingHand(true)
        );

        Burst(
            count: 1,
            interval: Ticks(1)
        );

        Projectile(
            speed: BlocksPerTick(Speed),
            range: Blocks(Range),
            radius: HitRadius,
            gravity: 0.0,
            pierce: 0
        );

        Collision(
            blocks: STOP,
            entities: FIRST_VALID_TARGET
        );

        Targets(LIVING_EXCEPT_CASTER);
        FriendlyFire(false);

        Trail(
            particle: AirCircle,
            spacing: 0.15,
            size: 0.22,
            lifetime: Ticks(10),
            color: Color("#FFFFFF"),
            vanillaColor: Color("#A8D8FF"),
            alphaStart: 0.86,
            alphaEnd: 0.0
        );

        Core(
            particle: AirCircle,
            size: 0.16,
            color: Color("#FFFFFF"),
            vanillaColor: Color("#E8F7FF")
        );

        Hit(
            Damage(
                type: AirDamage,
                amount: DamageAmount
            ),
            Knockback(strength: 1.0, direction: FORWARD)
        );
    }
}
