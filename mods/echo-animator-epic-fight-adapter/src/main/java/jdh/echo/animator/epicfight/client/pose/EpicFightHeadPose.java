package jdh.echo.animator.epicfight.client.pose;

import jdh.echo.animator.epicfight.EchoAnimatorEpicFightMod;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public final class EpicFightHeadPose {
    private static final String HEAD_JOINT = "Head";
    private static final String ROOT_JOINT = "Root";

    private final Map<Class<? extends Armature>, Integer> headJointIds = new ConcurrentHashMap<>();
    private final Map<Class<? extends Armature>, Integer> rootJointIds = new ConcurrentHashMap<>();
    private final Set<Class<? extends Armature>> warnedMissingHead = ConcurrentHashMap.newKeySet();
    private final Set<Class<? extends Armature>> warnedMissingRoot = ConcurrentHashMap.newKeySet();
    private final Set<Class<? extends Armature>> warnedBadHeadMatrixIndex = ConcurrentHashMap.newKeySet();
    private final Set<Class<? extends Armature>> warnedBadRootMatrixIndex = ConcurrentHashMap.newKeySet();

    public OpenMatrix4f resolveHead(LivingEntityPatch<?> patch, OpenMatrix4f[] poseMatrices) {
        return resolveJoint(patch, poseMatrices, HEAD_JOINT, headJointIds,
                warnedMissingHead, warnedBadHeadMatrixIndex);
    }

    public OpenMatrix4f resolveRoot(LivingEntityPatch<?> patch, OpenMatrix4f[] poseMatrices) {
        return resolveJoint(patch, poseMatrices, ROOT_JOINT, rootJointIds,
                warnedMissingRoot, warnedBadRootMatrixIndex);
    }

    private OpenMatrix4f resolveJoint(
            LivingEntityPatch<?> patch,
            OpenMatrix4f[] poseMatrices,
            String jointName,
            Map<Class<? extends Armature>, Integer> jointIds,
            Set<Class<? extends Armature>> warnedMissingJoint,
            Set<Class<? extends Armature>> warnedBadMatrixIndex) {
        if (patch == null || poseMatrices == null) {
            return null;
        }

        Armature armature = patch.getArmature();
        if (armature == null) {
            return null;
        }

        Class<? extends Armature> armatureClass = armature.getClass();
        int jointId = jointIds.computeIfAbsent(armatureClass,
                ignored -> findJointId(armature, jointName, warnedMissingJoint));
        if (jointId < 0) {
            return null;
        }
        if (jointId >= poseMatrices.length) {
            warnBadMatrixIndex(armatureClass, jointName, jointId, poseMatrices.length, warnedBadMatrixIndex);
            return null;
        }
        return poseMatrices[jointId];
    }

    private int findJointId(
            Armature armature,
            String jointName,
            Set<Class<? extends Armature>> warnedMissingJoint) {
        Joint joint;
        try {
            joint = armature.searchJointByName(jointName);
        } catch (RuntimeException exception) {
            warnMissingJoint(armature.getClass(), jointName, exception, warnedMissingJoint);
            return -1;
        }
        if (joint == null) {
            warnMissingJoint(armature.getClass(), jointName, null, warnedMissingJoint);
            return -1;
        }
        return joint.getId();
    }

    private void warnMissingJoint(
            Class<? extends Armature> armatureClass,
            String jointName,
            RuntimeException exception,
            Set<Class<? extends Armature>> warnedMissingJoint) {
        if (!warnedMissingJoint.add(armatureClass)) {
            return;
        }
        if (exception == null) {
            EchoAnimatorEpicFightMod.LOGGER.warn(
                    "Epic Fight armature {} has no {} joint; using original Echo face layer",
                    armatureClass.getName(),
                    jointName);
            return;
        }
        EchoAnimatorEpicFightMod.LOGGER.warn(
                "Could not resolve Epic Fight {} joint for {}; using original Echo face layer",
                jointName,
                armatureClass.getName(),
                exception);
    }

    private void warnBadMatrixIndex(
            Class<? extends Armature> armatureClass,
            String jointName,
            int jointId,
            int matrixCount,
            Set<Class<? extends Armature>> warnedBadMatrixIndex) {
        if (warnedBadMatrixIndex.add(armatureClass)) {
            EchoAnimatorEpicFightMod.LOGGER.warn(
                    "Epic Fight {} joint id {} is outside poseMatrices length {} for {}; "
                            + "using original Echo face layer",
                    jointName,
                    jointId,
                    matrixCount,
                    armatureClass.getName());
        }
    }
}
