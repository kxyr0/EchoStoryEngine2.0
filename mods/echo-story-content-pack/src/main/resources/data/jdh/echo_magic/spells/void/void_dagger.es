namespace EchoMagicWeapons.Spells;

class VoidDaggerSpell : SpellResource
{
    private void Init()
    {
        Id("echo_magic_weapons:void_dagger");
        DisplayName("Void Dagger");
        School("echo_void_magic:void");
        Technique(WEAPON);
        Cooldown(Ticks(40));
        Cast(Origin(PLAYER_CENTER), Direction(PLAYER_LOOK), Hand(MAIN_HAND), SwingHand(true));
        Burst(count: 1, interval: Ticks(1));
        Projectile(speed: BlocksPerTick(0.01), range: Blocks(0.1), radius: 0.01, gravity: 0.0, pierce: 0);
        Collision(blocks: STOP, entities: FIRST_VALID_TARGET);
        Targets(LIVING_EXCEPT_CASTER);
        FriendlyFire(false);
        Trail(particle: "echo_void_magic:void_circle", spacing: 0.1, size: 0.05,
            lifetime: Ticks(2), color: Color("#0B632C"), vanillaColor: Color("#0B632C"),
            alphaStart: 0.1, alphaEnd: 0.0);
        Core(particle: "echo_void_magic:void_circle", size: 0.04,
            color: Color("#39FF75"), vanillaColor: Color("#39FF75"));
        Hit(Damage(type: "echo_magic_weapons:weapon_magic", amount: 0.01));
    }
}
