namespace EchoFireMagic.Spells;

class FireBoltSpell : SpellResource
{
    private SpellId FireBolt =
        "echo_fire_magic:fire_bolt";

    private MagicSchoolId FireMagic =
        "echo_fire_magic:fire";

    private MagicDamageTypeId FireDamage =
        "echo_fire_magic:fire_magic";

    private MagicParticleStyleId FireFlame =
        "echo_fire_magic:fire_flame";

    private double Damage = 4.0;
    private double Speed = 1.35;
    private double Range = 22.0;
    private double HitRadius = 0.26;

    private void Init()
    {
        Id(FireBolt);
        DisplayName("Fire Bolt");
        School(FireMagic);
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
            particle: FireFlame,
            spacing: 0.16,
            size: 0.24,
            lifetime: Ticks(10),
            color: Color("#FFFFFF"),
            vanillaColor: Color("#FF5A18"),
            alphaStart: 0.92,
            alphaEnd: 0.0
        );

        Core(
            particle: FireFlame,
            size: 0.18,
            color: Color("#FFF2B2"),
            vanillaColor: Color("#FFD35A")
        );

        Hit(
            Damage(
                type: FireDamage,
                amount: Damage
            ),
            Ignite(duration: Ticks(80))
        );
    }
}
