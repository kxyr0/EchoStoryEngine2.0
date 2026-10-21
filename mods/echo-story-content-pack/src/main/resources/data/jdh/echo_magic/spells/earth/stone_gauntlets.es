namespace EchoMagicWeapons.Spells;

class StoneGauntletsSpell : SpellResource
{
    private void Init()
    {
        Id("echo_magic_weapons:stone_gauntlets");
        DisplayName("Stone Gauntlets");
        School("echo_earth_magic:earth");
        Technique(WEAPON);
        Cooldown(Ticks(40));
        Cast(Origin(PLAYER_CENTER), Direction(PLAYER_LOOK), Hand(MAIN_HAND), SwingHand(true));
        Burst(count: 1, interval: Ticks(1));
        Projectile(speed: BlocksPerTick(0.01), range: Blocks(0.1), radius: 0.01, gravity: 0.0, pierce: 0);
        Collision(blocks: STOP, entities: FIRST_VALID_TARGET);
        Targets(LIVING_EXCEPT_CASTER);
        FriendlyFire(false);
        Trail(particle: "minecraft:cloud", spacing: 0.1, size: 0.05,
            lifetime: Ticks(2), color: Color("#B6A477"), vanillaColor: Color("#B6A477"),
            alphaStart: 0.1, alphaEnd: 0.0);
        Core(particle: "minecraft:cloud", size: 0.04,
            color: Color("#E1D4A8"), vanillaColor: Color("#E1D4A8"));
        Hit(Damage(type: "echo_magic_weapons:weapon_magic", amount: 0.01));
    }
}
