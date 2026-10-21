namespace EchoWaterMagic.Spells;

class WaterRingSpell : SpellResource
{
    private MagicSchoolId WaterMagic = "echo_water_magic:water";
    private MagicDamageTypeId WaterDamage = "echo_water_magic:water_magic";
    private MagicParticleStyleId WaterCircle = "echo_water_magic:water_circle";

    private void Init()
    {
        Id("echo_water_magic:water_ring");
        DisplayName("Water Ring");
        School(WaterMagic);
        Technique(CIRCLE);
        Cooldown(Ticks(46));
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
            speed: BlocksPerTick(0.62),
            range: Blocks(5.5),
            radius: 0.34,
            gravity: 0.0,
            pierce: 0
        );
        Collision(blocks: STOP, entities: FIRST_VALID_TARGET);
        Targets(LIVING_EXCEPT_CASTER);
        FriendlyFire(false);
        Trail(
            particle: WaterCircle,
            spacing: 0.12,
            size: 0.3,
            lifetime: Ticks(13),
            color: Color("#FFFFFF"),
            vanillaColor: Color("#168FEA"),
            alphaStart: 0.94,
            alphaEnd: 0.0
        );
        Core(particle: WaterCircle, size: 0.2, color: Color("#FFFFFF"),
            vanillaColor: Color("#39E8FF"));
        Hit(
            Damage(type: WaterDamage, amount: 0.5),
            Extinguish(true),
            Knockback(strength: 1.0, direction: FORWARD)
        );
    }
}
