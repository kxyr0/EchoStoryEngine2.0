package io.github.kxyr0.echoplayerfollow.client.targeting;

import io.github.kxyr0.echoplayerfollow.client.ClientFollowState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;

public final class FollowTargeting {
    private FollowTargeting() {
    }

    public static boolean canUsePrompt(ClientFollowState.Prompt prompt) {
        return targetedEntityId() == prompt.entityId();
    }

    public static int targetedEntityId() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.hitResult instanceof EntityHitResult hit)) {
            return -1;
        }
        Entity entity = hit.getEntity();
        return entity.getId();
    }
}
