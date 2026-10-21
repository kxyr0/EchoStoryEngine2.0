package io.github.kxyr0.echomagic.echoscript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echomagic.echoscript.binding.SpellDefinitionBinder;
import io.github.kxyr0.echomagic.api.gesture.MagicTechnique;
import io.github.kxyr0.echomagic.api.hit.SpellKnockbackDirection;
import io.github.kxyr0.echomagic.api.visual.MagicColor;
import io.github.kxyr0.echomagic.echoscript.diagnostic.EchoScriptDiagnosticAdapter;
import io.github.kxyr0.echoscript.api.EchoScriptParser;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class EchoScriptSpellCompilerTest {
    private static final String PACK_SPELLS =
            "src/main/resources/data/jdh/echo_magic/spells/";

    @Test void initOnlyVoidDaggersShapeParsesBindsAndValidatesOnce() {
        AtomicInteger calls = new AtomicInteger();
        EchoScriptParser delegate = EchoScriptLanguage.parser();
        EchoScriptParser counting = request -> { calls.incrementAndGet(); return delegate.parse(request); };
        var result = new EchoScriptSpellCompiler(counting, new EchoScriptDiagnosticAdapter(),
                new SpellDefinitionBinder()).compile("test:void_daggers.es", source());
        assertTrue(result.definition().isPresent(), () -> result.diagnostics().toString());
        assertEquals(1, calls.get());
        var spell = result.definition().orElseThrow();
        assertEquals("test:void_daggers", spell.id().value());
        assertEquals(3, spell.burst().count());
        assertEquals(2, spell.burst().intervalTicks());
        assertEquals(72, spell.burst().spreadDegrees());
        assertEquals(1.6, spell.projectile().speed());
        assertEquals(24, spell.projectile().range());
        assertEquals(0.22, spell.projectile().radius());
        assertEquals(2, spell.hit().amount());
        assertEquals(80, spell.hit().igniteTicks());
        assertTrue(spell.hit().extinguish());
        assertEquals(1.1, spell.hit().knockback());
        assertEquals(SpellKnockbackDirection.FORWARD, spell.hit().knockbackDirection());
        assertTrue(spell.hit().deflectProjectiles());
        assertEquals(MagicTechnique.ARC, spell.technique());
        assertEquals(16, spell.cooldown().ticks());
        assertEquals(MagicColor.fromHex("#112233"), spell.trail().vanillaColor());
        assertEquals(MagicColor.fromHex("#AABBCC"), spell.core().vanillaColor());
        assertEquals("test:core", spell.core().particle().value());
    }

    @Test void wrongResourceBaseIsRejectedByBinder() {
        var result = new EchoScriptSpellCompiler().compile("wrong.es",
                "class WrongResource : SomethingElse { private void Init() { } }");
        assertTrue(result.definition().isEmpty());
    }

    @Test void siblingVoidContentResourceCompilesWhenAvailable() throws IOException {
        Path content = locateVoidContent();
        Assumptions.assumeTrue(content != null, "echo-void-magic is not checked out beside echo-magic");
        var result = new EchoScriptSpellCompiler().compile(content.toString(), Files.readString(content));
        assertTrue(result.definition().isPresent(), () -> result.diagnostics().toString());
        var spell = result.definition().orElseThrow();
        assertEquals("echo_void_magic:void_daggers", spell.id().value());
        assertEquals("echo_void_magic:void", spell.school().value());
        assertEquals("echo_void_magic:void_magic", spell.hit().damageType().value());
        assertEquals("echo_void_magic:void_circle", spell.trail().particle().value());
    }

    @Test void everyStoryContentPackSpellCompilesWithUniqueId() throws IOException {
        Path root = locatePackSpells();
        Assumptions.assumeTrue(root != null, "echo-story-content-pack is not checked out beside echo-magic");
        Set<String> ids = new HashSet<>();
        try (var paths = Files.walk(root)) {
            for (Path script : paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".es")).sorted().toList()) {
                var result = new EchoScriptSpellCompiler().compile(script.toString(), Files.readString(script));
                assertTrue(result.definition().isPresent(), () -> result.diagnostics().toString());
                assertTrue(ids.add(result.definition().orElseThrow().id().value()), "Duplicate spell id in " + script);
            }
        }
        assertEquals(19, ids.size());
        assertTrue(ids.contains("echo_blood_magic:blood_rapier"));
        assertTrue(ids.contains("echo_magic_weapons:stone_gauntlets"));
    }

    @Test void siblingArcContentResourcesCompileWhenAvailable() throws IOException {
        assertArc("echo-story-content-pack", PACK_SPELLS + "fire/fire_sweep.es",
                "echo_fire_magic:fire_sweep", 82.0D);
        assertArc("echo-story-content-pack", PACK_SPELLS + "water/water_wave.es",
                "echo_water_magic:water_wave", 100.0D);
        assertArc("echo-story-content-pack", PACK_SPELLS + "air/air_arc.es",
                "echo_air_magic:air_arc", 108.0D);
    }

    @Test void siblingCircleContentResourcesCompileWhenAvailable() throws IOException {
        assertTechnique("echo-story-content-pack", PACK_SPELLS + "fire/fire_ring.es",
                "echo_fire_magic:fire_ring", MagicTechnique.CIRCLE, 337.5D);
        assertTechnique("echo-story-content-pack", PACK_SPELLS + "water/water_ring.es",
                "echo_water_magic:water_ring", MagicTechnique.CIRCLE, 337.5D);
        assertTechnique("echo-story-content-pack", PACK_SPELLS + "air/air_ring.es",
                "echo_air_magic:air_ring", MagicTechnique.CIRCLE, 337.5D);
    }

    @Test void blacksmithHandPointResourceCompilesWhenAvailable() throws IOException {
        assertTechnique("echo-story-content-pack", PACK_SPELLS + "fire/blacksmith_hand.es",
                "echo_fire_magic:blacksmith_hand",
                MagicTechnique.POINT, 0.0D);
    }

    @Test void voidStepPointResourceCompilesWhenAvailable() throws IOException {
        assertTechnique("echo-story-content-pack", PACK_SPELLS + "void/void_step.es",
                "echo_void_magic:void_step",
                MagicTechnique.POINT, 0.0D);
    }

    private void assertArc(String repository, String resource, String id, double spread) throws IOException {
        assertTechnique(repository, resource, id, MagicTechnique.ARC, spread);
    }

    private void assertTechnique(String repository, String resource, String id, MagicTechnique technique,
                                 double spread) throws IOException {
        Path content = locateSibling(repository, resource);
        Assumptions.assumeTrue(content != null, repository + " is not checked out beside echo-magic");
        var result = new EchoScriptSpellCompiler().compile(content.toString(), Files.readString(content));
        assertTrue(result.definition().isPresent(), () -> result.diagnostics().toString());
        var spell = result.definition().orElseThrow();
        assertEquals(id, spell.id().value());
        assertEquals(technique, spell.technique());
        assertEquals(spread, spell.burst().spreadDegrees());
    }

    private Path locateVoidContent() {
        return locateSibling("echo-story-content-pack", PACK_SPELLS + "void/void_daggers.es");
    }

    private Path locatePackSpells() {
        for (Path candidate : new Path[] {Path.of("..", "echo-story-content-pack", PACK_SPELLS),
                Path.of("..", "..", "echo-story-content-pack", PACK_SPELLS)}) {
            Path normalized = candidate.toAbsolutePath().normalize();
            if (Files.isDirectory(normalized)) return normalized;
        }
        return null;
    }

    private Path locateSibling(String repository, String resource) {
        for (Path candidate : new Path[] {Path.of("..", repository, resource),
                Path.of("..", "..", repository, resource)}) {
            Path normalized = candidate.toAbsolutePath().normalize();
            if (Files.isRegularFile(normalized)) return normalized;
        }
        return null;
    }

    private String source() {
        return """
                namespace Test.Spells;
                class VoidDaggersSpell : SpellResource
                {
                    private SpellId Spell = "test:void_daggers";
                    private MagicSchoolId SchoolId = "test:void";
                    private MagicDamageTypeId DamageId = "test:void_magic";
                    private MagicParticleStyleId Particle = "test:circle";
                    private MagicParticleStyleId CoreParticle = "test:core";
                    private int Count = 3;
                    private double Amount = 2.0;
                    private double Speed = 1.6;
                    private double Range = 24.0;
                    private double Radius = 0.22;
                    private void Init()
                    {
                        Id(Spell);
                        DisplayName("Void Daggers");
                        School(SchoolId);
                        Technique(ARC);
                        Cooldown(Ticks(16));
                        Cast(Origin(PLAYER_EYES), Direction(PLAYER_LOOK), Hand(MAIN_HAND), SwingHand(true));
                        Burst(count: Count, interval: Ticks(2), spread: Degrees(72));
                        Projectile(speed: BlocksPerTick(Speed), range: Blocks(Range), radius: Radius, gravity: 0.0, pierce: 0);
                        Collision(blocks: STOP, entities: FIRST_VALID_TARGET);
                        Targets(LIVING_EXCEPT_CASTER);
                        FriendlyFire(false);
                        Trail(particle: Particle, spacing: 0.18, size: 0.30, lifetime: Ticks(8),
                            color: Color("#0F6B32"), alphaStart: 0.90, alphaEnd: 0.0,
                            vanillaColor: Color("#112233"));
                        Core(particle: CoreParticle, size: 0.16, color: Color("#45FF4A"),
                            vanillaColor: Color("#AABBCC"));
                        Hit(Damage(type: DamageId, amount: Amount), Ignite(duration: Ticks(80)),
                            Extinguish(true), Knockback(strength: 1.1, direction: FORWARD),
                            DeflectProjectiles(true));
                    }
                }
                """;
    }
}
