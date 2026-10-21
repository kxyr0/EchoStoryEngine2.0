package io.github.kxyr0.echoitemattachments.player;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;

final class PlayerAttachmentPoints {
    private PlayerAttachmentPoints() {
    }

    static AttachmentPointId physical(Player player, InteractionHand hand) {
        boolean mainHand = hand == InteractionHand.MAIN_HAND;
        boolean mainRight = player.getMainArm() == HumanoidArm.RIGHT;
        return mainHand == mainRight ? AttachmentPointId.RIGHT_HAND : AttachmentPointId.LEFT_HAND;
    }
}
