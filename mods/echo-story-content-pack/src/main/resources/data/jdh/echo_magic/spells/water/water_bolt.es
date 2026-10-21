namespace EchoWaterMagic.Spells;

class WaterBoltSpell : SpellResource
{
    private SpellId WaterBolt =
        "echo_water_magic:water_bolt";

    private MagicSchoolId WaterMagic =
        "echo_water_magic:water";

    private MagicDamageTypeId WaterDamage =
        "echo_water_magic:water_magic";

    private MagicParticleStyleId WaterCircle =
        "echo_water_magic:water_circle";

    private double DamageAmount = 4.0;
    private double Speed = 1.35;
    private double Range = 22.0;
    private double HitRadius = 0.26;

    private void Init()
    {
        Id(WaterBolt);
        DisplayName("Water Bolt");
        School(WaterMagic);
        Technique(LINE);

        Cooldown(Ticks(20));

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
            particle: WaterCircle,
            spacing: 0.16,
            size: 0.24,
            lifetime: Ticks(10),
            color: Color("#FFFFFF"),
            vanillaColor: Color("#168FEA"),
            alphaStart: 0.92,
            alphaEnd: 0.0
        );

        Core(
            particle: WaterCircle,
            size: 0.18,
            color: Color("#FFFFFF"),
            vanillaColor: Color("#39E8FF")
        );

        Hit(
            Damage(
                type: WaterDamage,
                amount: DamageAmount
            ),
            Extinguish(true),
            Knockback(strength: 0.85, direction: FORWARD)
        );
    }
}
