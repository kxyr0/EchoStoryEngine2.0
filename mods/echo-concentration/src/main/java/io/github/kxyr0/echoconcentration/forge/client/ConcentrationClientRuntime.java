package io.github.kxyr0.echoconcentration.forge.client;

import io.github.kxyr0.echoconcentration.api.ConcentrationRevision;
import io.github.kxyr0.echoconcentration.api.ConcentrationService;
import io.github.kxyr0.echoconcentration.api.ConcentrationSnapshot;
import io.github.kxyr0.echoconcentration.api.ConcentrationState;
import io.github.kxyr0.echoconcentration.api.lifecycle.ConcentrationLifecycleEvent;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;

public final class ConcentrationClientRuntime implements ConcentrationService {
    private final ConcentrationPostEffect effect = new ConcentrationPostEffect();
    private final ConcentrationSoundDucker sound = new ConcentrationSoundDucker();
    private final ConcentrationPresentationConflicts conflicts = new ConcentrationPresentationConflicts();
    private ConcentrationSnapshot snapshot = new ConcentrationSnapshot(
            ConcentrationState.INACTIVE, new ConcentrationRevision(0L));

    @Override
    public ConcentrationSnapshot snapshot() {
        return snapshot;
    }

    @Override
    public boolean enter() {
        Minecraft minecraft = Minecraft.getInstance();
        if (snapshot.active() || minecraft.player == null || minecraft.level == null
                || conflicts.blocksEntry(minecraft)) {
            return false;
        }
        transition(ConcentrationState.ACTIVE, ConcentrationLifecycleEvent.Kind.ENTERED);
        effect.enter(minecraft);
        return true;
    }

    @Override
    public boolean exit() {
        if (!snapshot.active()) {
            return false;
        }
        effect.exit(Minecraft.getInstance());
        transition(ConcentrationState.INACTIVE, ConcentrationLifecycleEvent.Kind.EXITED);
        return true;
    }

    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (snapshot.active() && (minecraft.player == null || minecraft.level == null
                || conflicts.requiresExit(minecraft))) {
            leaveImmediately();
        }
        effect.tick(minecraft, snapshot.active());
        sound.tick(minecraft, snapshot.active());
    }

    public void capturePlayer(RenderLevelStageEvent event) {
        if (snapshot.active()) {
            effect.capturePlayer(Minecraft.getInstance(), event);
        }
    }

    public void keyPressed(int keyCode, int scanCode) {
        if (!ConcentrationKeyMappings.matches(keyCode, scanCode)) {
            return;
        }
        if (snapshot.active()) {
            exit();
        } else {
            enter();
        }
    }

    public void clear() {
        if (snapshot.active()) {
            transition(ConcentrationState.INACTIVE, ConcentrationLifecycleEvent.Kind.EXITED);
        }
        effect.release(Minecraft.getInstance());
        sound.release(Minecraft.getInstance());
    }

    private void leaveImmediately() {
        effect.release(Minecraft.getInstance());
        sound.release(Minecraft.getInstance());
        transition(ConcentrationState.INACTIVE, ConcentrationLifecycleEvent.Kind.EXITED);
    }

    private void transition(ConcentrationState state, ConcentrationLifecycleEvent.Kind kind) {
        snapshot = new ConcentrationSnapshot(state, snapshot.revision().next());
        MinecraftForge.EVENT_BUS.post(new ConcentrationLifecycleEvent(kind, snapshot));
    }
}
