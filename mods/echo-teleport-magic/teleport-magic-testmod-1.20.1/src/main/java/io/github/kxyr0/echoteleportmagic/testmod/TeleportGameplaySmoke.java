package io.github.kxyr0.echoteleportmagic.testmod;

import io.github.kxyr0.echoteleportmagic.api.TeleportDestination;
import io.github.kxyr0.echoteleportmagic.api.TeleportExecutionId;
import io.github.kxyr0.echoteleportmagic.api.TeleportState;
import io.github.kxyr0.echoteleportmagic.forge.EchoTeleportMagic;
import io.github.kxyr0.echoteleportmagic.forge.server.TeleportSubjects;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Exercises actual server entities, seat preservation, native Player teleport and NPC durable mutation. */
@Mod.EventBusSubscriber(modid = "echo_teleport_magic_test")
public final class TeleportGameplaySmoke {
    private static Check pending;
    private TeleportGameplaySmoke() { }
    public static void start(MinecraftServer server, UUID subject) {
        pending = new Check(server, TeleportSubjects.require(server, subject));
        pending.begin();
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (Boolean.getBoolean("etm.serverSmoke") && pending == null) {
            var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null && server.overworld().getGameTime() > 10) {
                write("etm-dedicated-smoke.txt", "DEDICATED_SERVER_STARTED; active=" + EchoTeleportMagic.service().coordinator().active().size());
                server.halt(false);
            }
        }
        if (pending == null) return;
        try { pending.tick(); }
        catch (RuntimeException failure) { write("etm-gameplay-smoke.txt", "FAIL: " + failure); stop(); }
    }
    private static void write(String file, String text) {
        try { Files.writeString(Path.of(file), text + "\n"); }
        catch (java.io.IOException failure) { throw new IllegalStateException(failure); }
    }
    private static void stop() {
        pending = null;
        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> net.minecraft.client.Minecraft.getInstance().execute(() -> net.minecraft.client.Minecraft.getInstance().stop()));
    }
    private static final class Check {
        private final MinecraftServer server;
        private final LivingEntity subject;
        private final Vec3 source;
        private final boolean seated;
        private final float health;
        private long start;
        private int completions;
        private boolean checked;
        Check(MinecraftServer server, LivingEntity subject) {
            this.server = server; this.subject = subject;
            source = subject.position(); seated = subject.isPassenger(); health = subject.getHealth();
        }
        void begin() {
            start = server.overworld().getGameTime();
            startOne(subject, "subject");
            startOne(server.getPlayerList().getPlayers().get(0), "player");
        }
        private void startOne(LivingEntity entity, String label) {
            var id = TeleportExecutionId.owned("etm-live-gameplay", label);
            var destination = new TeleportDestination("debug:" + label, entity.level().dimension().location().toString(),
                    entity.getX() + 8, entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
            EchoTeleportMagic.service().start(entity, destination, id, "etm-live-gameplay", "testmod", false, result -> {
                require(result.state() == TeleportState.COMPLETED && result.committed(), "Commit failed: " + result.state());
                require(Math.abs(entity.getX() - destination.x()) < .01, "Entity did not arrive");
                var duplicate = EchoTeleportMagic.service().start(entity, destination, id, "etm-live-gameplay", "testmod", false, ignored -> { });
                require(duplicate.state() == TeleportState.COMPLETED, "Duplicate request restarted");
                completions++;
            });
        }
        void tick() {
            long elapsed = server.overworld().getGameTime() - start;
            int commit = io.github.kxyr0.echoteleportmagic.timeline.TeleportTimeline.COMMIT_TICK;
            if (!checked && elapsed >= commit - 4 && elapsed < commit) {
                require(subject.position().distanceToSqr(source) < .001, "Position changed before concealment");
                require(subject.isPassenger() == seated, "Seat released before concealment");
                checked = true;
            }
            if (completions == 2) {
                require(checked, "Precommit checkpoint not observed");
                require(!subject.isPassenger(), "Seat not released at commit");
                require(subject.getHealth() == health, "Health mutated");
                if (net.minecraftforge.fml.ModList.get().isLoaded("echo_npc")) NpcSmokeFixture.assertPosition(server, source.x + 8);
                write("etm-gameplay-smoke.txt", "PASS: actual Player + " + (net.minecraftforge.fml.ModList.get().isLoaded("echo_npc") ? "Echo NPC" : "ArmorStand")
                        + "; seated=" + seated + "; precommit pose/position retained; destinations correct; duplicate requests suppressed; health unchanged");
                stop();
            } else if (elapsed > 180) throw new IllegalStateException("Gameplay smoke timed out; completed=" + completions);
        }
        private void require(boolean condition, String detail) { if (!condition) throw new IllegalStateException(detail); }
    }
}
