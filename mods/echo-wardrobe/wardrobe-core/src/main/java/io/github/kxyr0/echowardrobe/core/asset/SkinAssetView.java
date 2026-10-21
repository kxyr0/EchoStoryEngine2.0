package io.github.kxyr0.echowardrobe.core.asset;

/**
 * Pure boundary for validating referenced wardrobe skin resources.
 */
@FunctionalInterface
public interface SkinAssetView {
    boolean skinExists(String resourceId);
}
