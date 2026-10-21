namespace EchoWaterMagic.Spells;

class WaterWaveSpell : SpellResource
{
    private MagicSchoolId WaterMagic = "echo_water_magic:water";
    private MagicDamageTypeId WaterDamage = "echo_water_magic:water_magic";
    private MagicParticleStyleId WaterCircle = "echo_water_magic:water_circle";

    private void Init()
    {
        Id("echo_water_magic:water_wave");
        DisplayName("Water Wave");
        School(WaterMagic);
        Technique(ARC);
        Cooldown(Ticks(32));
        Cast(
            Origin(PLAYER_EYES),
            Direction(PLAYER_LOOK),
            Hand(MAIN_HAND),
            SwingHand(true)
        );
        Burst(
            count: 9,
            interval: Ticks(1),
            spread: Degrees(100)
        );
        Projectile(
            speed: BlocksPerTick(0.85),
            range: Blocks(9.0),
            radius: 0.4,
            gravity: 0.0,
            pierce: 0
        );
        Collision(blocks: STOP, entities: FIRST_VALID_TARGET);
        Targets(LIVING_EXCEPT_CASTER);
        FriendlyFire(false);
        Trail(
            particle: WaterCircle,
            spacing: 0.13,
            size: 0.32,
            lifetime: Ticks(12),
            color: Color("#FFFFFF"),
            vanillaColor: Color("#168FEA"),
            alphaStart: 0.94,
            alphaEnd: 0.0
        );
        Core(particle: WaterCircle, size: 0.22, color: Color("#FFFFFF"),
            vanillaColor: Color("#39E8FF"));
        Hit(
            Damage(type: WaterDamage, amount: 0.75),
            Extinguish(true),
            Knockback(strength: 1.15, direction: FORWARD)
        );
    }
}
