namespace EchoVoidMagic.Spells;

class VoidDaggersSpell : SpellResource
{
    private SpellId VoidDaggers =
        "echo_void_magic:void_daggers";

    private MagicSchoolId VoidMagic =
        "echo_void_magic:void";

    private MagicDamageTypeId VoidDamage =
        "echo_void_magic:void_magic";

    private MagicParticleStyleId VoidCircle =
        "echo_void_magic:void_circle";

    private int DaggerCount = 3;
    private double DamagePerDagger = 2.0;
    private double Speed = 1.6;
    private double Range = 24.0;
    private double HitRadius = 0.22;

    private void Init()
    {
        Id(VoidDaggers);
        DisplayName("Void Daggers");
        School(VoidMagic);

        Cooldown(Ticks(16));

        Cast(
            Origin(PLAYER_EYES),
            Direction(PLAYER_LOOK),
            Hand(MAIN_HAND),
            SwingHand(true)
        );

        Burst(
            count: DaggerCount,
            interval: Ticks(2)
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
            particle: VoidCircle,
            spacing: 0.18,
            size: 0.30,
            lifetime: Ticks(8),
            color: Color("#0F6B32"),
            vanillaColor: Color("#0F6B32"),
            alphaStart: 0.90,
            alphaEnd: 0.0
        );

        Core(
            particle: VoidCircle,
            size: 0.16,
            color: Color("#45FF4A"),
            vanillaColor: Color("#45FF4A")
        );

        Hit(
            Damage(
                type: VoidDamage,
                amount: DamagePerDagger
            )
        );
    }
}
