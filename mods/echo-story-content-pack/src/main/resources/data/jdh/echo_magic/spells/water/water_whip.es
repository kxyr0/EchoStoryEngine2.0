namespace EchoMagicWeapons.Spells;

class WaterWhipSpell : SpellResource
{
    private void Init()
    {
        Id("echo_magic_weapons:water_whip");
        DisplayName("Water Whip");
        School("echo_water_magic:water");
        Technique(WEAPON);
        Cooldown(Ticks(40));
        Cast(Origin(PLAYER_CENTER), Direction(PLAYER_LOOK), Hand(MAIN_HAND), SwingHand(true));
        Burst(count: 1, interval: Ticks(1));
        Projectile(speed: BlocksPerTick(0.01), range: Blocks(0.1), radius: 0.01, gravity: 0.0, pierce: 0);
        Collision(blocks: STOP, entities: FIRST_VALID_TARGET);
        Targets(LIVING_EXCEPT_CASTER);
        FriendlyFire(false);
        Trail(particle: "echo_water_magic:water_circle", spacing: 0.1, size: 0.05,
            lifetime: Ticks(2), color: Color("#28AAFF"), vanillaColor: Color("#28AAFF"),
            alphaStart: 0.1, alphaEnd: 0.0);
        Core(particle: "echo_water_magic:water_circle", size: 0.04,
            color: Color("#B8F4FF"), vanillaColor: Color("#B8F4FF"));
        Hit(Damage(type: "echo_magic_weapons:weapon_magic", amount: 0.01));
    }
}
