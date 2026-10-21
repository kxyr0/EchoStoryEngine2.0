package io.github.kxyr0.echoik.animationadapter;

import java.util.Objects;

/**
 * Names one two-bone chain and its end effector in Echo Animation local ids.
 */
public record EchoIkChain(String rootBoneId, String midBoneId, String endEffectorId) {
    public EchoIkChain {
        rootBoneId = require(rootBoneId, "rootBoneId");
        midBoneId = require(midBoneId, "midBoneId");
        endEffectorId = require(endEffectorId, "endEffectorId");
    }

    private static String require(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return checked;
    }
}
