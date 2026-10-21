package io.github.kxyr0.echothoughtspace.forge.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

public final class ThoughtSpaceInputCapture {
    private boolean active;

    public void acquire(Minecraft minecraft) {
        active = true;
        clear(minecraft);
    }

    public void maintain(Minecraft minecraft) {
        if (active) clear(minecraft);
    }

    public void release() { active = false; }
    public boolean active() { return active; }

    private static void clear(Minecraft minecraft) {
        KeyMapping.releaseAll();
        Options options = minecraft.options;
        clear(options.keyUp, options.keyLeft, options.keyDown, options.keyRight, options.keyJump,
                options.keyShift, options.keySprint, options.keyInventory, options.keySwapOffhand,
                options.keyDrop, options.keyUse, options.keyAttack, options.keyPickItem,
                options.keySaveHotbarActivator, options.keyLoadHotbarActivator);
        for (KeyMapping mapping : options.keyHotbarSlots) clear(mapping);
        if (minecraft.player != null && minecraft.player.input != null) {
            minecraft.player.input.leftImpulse = 0.0F;
            minecraft.player.input.forwardImpulse = 0.0F;
            minecraft.player.input.jumping = false;
            minecraft.player.input.shiftKeyDown = false;
        }
    }

    private static void clear(KeyMapping... mappings) {
        for (KeyMapping mapping : mappings) {
            mapping.setDown(false);
            while (mapping.consumeClick()) { }
        }
    }
}

