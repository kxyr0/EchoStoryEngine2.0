package io.github.kxyr0.echomagic.epicfight.client;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

final class EpicFightHandAnchor {
    private EpicFightHandAnchor() { }

    static OpenMatrix4f resolve(PlayerPatch<?> patch, float partialTick) {
        return resolve(patch, InteractionHand.MAIN_HAND, partialTick);
    }

    static OpenMatrix4f resolve(PlayerPatch<?> patch, InteractionHand hand, float partialTick) {
        if (patch == null) return null;
        try {
            Armature armature = patch.getArmature();
            Joint tool = patch.getParentJointOfHand(hand);
            if (armature == null || tool == null) return null;
            // Epic Fight can return the root before the hand mapping is initialized.
            // Never anchor a weapon to that fallback or to a joint from another armature.
            if (tool == armature.rootJoint || !armature.hasJoint(tool.getName())) {
                boolean right = (hand == InteractionHand.MAIN_HAND)
                        == (patch.getOriginal().getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
                tool = armature.searchJointByName(right ? "Tool_R" : "Tool_L");
                if (tool == null) return null;
            }
            Pose pose = patch.getAnimator().getPose(partialTick);
            Vec3 position = patch.getOriginal().getPosition(partialTick);
            OpenMatrix4f model = new OpenMatrix4f().rotateDeg(180.0F, Vec3f.Y_AXIS)
                    .mulBack(patch.getModelMatrix(partialTick));
            OpenMatrix4f anchor = armature.getBoundTransformFor(pose, tool).mulFront(model);
            anchor.m30 += (float) position.x;
            anchor.m31 += (float) position.y;
            anchor.m32 += (float) position.z;
            return anchor;
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
