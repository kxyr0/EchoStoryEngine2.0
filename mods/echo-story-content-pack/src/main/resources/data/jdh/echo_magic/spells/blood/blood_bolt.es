namespace EchoBloodMagic.Spells;

class BloodBoltSpell : SpellResource
{
    private SpellId BloodBolt =
        "echo_blood_magic:blood_bolt";

    private MagicSchoolId BloodMagic =
        "echo_blood_magic:blood";

    private MagicDamageTypeId BloodDamage =
        "echo_blood_magic:blood_magic";

    private MagicParticleStyleId BloodCircle =
        "echo_blood_magic:blood_circle";

    private double DamageAmount = 5.0;
    private double Speed = 1.2;
    private double Range = 20.0;
    private double HitRadius = 0.28;

    private void Init()
    {
        Id(BloodBolt);
        DisplayName("Blood Bolt");
        School(BloodMagic);
        Technique(LINE);

        Cooldown(Ticks(24));

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
            particle: BloodCircle,
            spacing: 0.17,
            size: 0.26,
            lifetime: Ticks(10),
            color: Color("#FFFFFF"),
            vanillaColor: Color("#8B0000"),
            alphaStart: 0.94,
            alphaEnd: 0.0
        );

        Core(
            particle: BloodCircle,
            size: 0.18,
            color: Color("#FFFFFF"),
            vanillaColor: Color("#FF3030")
        );

        Hit(
            Damage(
                type: BloodDamage,
                amount: DamageAmount
            )
        );
    }
}
