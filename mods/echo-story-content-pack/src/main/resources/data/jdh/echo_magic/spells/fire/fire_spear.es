namespace EchoMagicWeapons.Spells;

class FireSpearSpell : SpellResource
{
    private void Init()
    {
        Id("echo_magic_weapons:fire_spear");
        DisplayName("Fire Spear");
        School("echo_fire_magic:fire");
        Technique(WEAPON);
        Cooldown(Ticks(40));
        Cast(Origin(PLAYER_CENTER), Direction(PLAYER_LOOK), Hand(MAIN_HAND), SwingHand(true));
        Burst(count: 1, interval: Ticks(1));
        Projectile(speed: BlocksPerTick(0.01), range: Blocks(0.1), radius: 0.01, gravity: 0.0, pierce: 0);
        Collision(blocks: STOP, entities: FIRST_VALID_TARGET);
        Targets(LIVING_EXCEPT_CASTER);
        FriendlyFire(false);
        Trail(particle: "echo_fire_magic:fire_flame", spacing: 0.1, size: 0.05,
            lifetime: Ticks(2), color: Color("#E51818"), vanillaColor: Color("#E51818"),
            alphaStart: 0.1, alphaEnd: 0.0);
        Core(particle: "echo_fire_magic:fire_flame", size: 0.04,
            color: Color("#FF493D"), vanillaColor: Color("#FF493D"));
        Hit(Damage(type: "echo_magic_weapons:weapon_magic", amount: 0.01));
    }
}
