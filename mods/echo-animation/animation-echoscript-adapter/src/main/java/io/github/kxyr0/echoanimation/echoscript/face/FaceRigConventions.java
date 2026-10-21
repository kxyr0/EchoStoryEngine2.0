package io.github.kxyr0.echoanimation.echoscript.face;

import io.github.kxyr0.echoanimation.api.math.IntRect;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.model.LayerRole;

/**
 * Centralized defaults and naming rules for high-level face rig syntax.
 */
final class FaceRigConventions {
    static final String FACE_ROOT = "face_root";
    private static final double EYE_LEFT_X = -0.125D;
    private static final double EYE_RIGHT_X = 0.125D;

    String defaultTextureRoot(String rigId) {
        int separator = rigId.indexOf(':');
        if (separator < 1 || separator == rigId.length() - 1) {
            throw new IllegalArgumentException("Face id must be namespaced: " + rigId);
        }
        return rigId.substring(0, separator) + ":echo_animation/faces/" + rigId.substring(separator + 1);
    }

    String texture(String root, FaceScriptModel.Part part) {
        return texturePath(root, part.settings().texture.orElse(defaultTextureName(part)));
    }

    String texturePath(String root, String value) {
        return value.contains(":") ? value : root + "/" + normalize(value);
    }

    String layerId(FaceScriptModel.Part part) {
        return switch (part.kind()) {
            case SCLERA -> "sclera_" + side(part.variant());
            case PUPIL -> "pupil_" + side(part.variant());
            case BROW -> "brow_" + side(part.variant());
            case EYELID -> lower(part.variant()) + "_eyelids";
        };
    }

    String splitEyelidLayerId(String variant, String side) {
        return lower(variant) + "_eyelid_" + side;
    }

    String boneId(FaceScriptModel.Part part) {
        return part.settings().bone.orElseGet(() -> switch (part.kind()) {
            case SCLERA -> side(part.variant()) + "_sclera";
            case PUPIL -> side(part.variant()) + "_pupil";
            case BROW -> "brows_" + side(part.variant());
            case EYELID -> lower(part.variant()) + "_eyelids";
        });
    }

    String defaultTextureName(FaceScriptModel.Part part) {
        return switch (part.kind()) {
            case EYELID -> lower(part.variant()) + "_eyelids";
            default -> layerId(part);
        };
    }

    LayerRole role(FaceScriptModel.Part part) {
        return part.settings().role.orElseGet(() -> switch (part.kind()) {
            case PUPIL -> LayerRole.PUPIL;
            case BROW -> LayerRole.BROW;
            case EYELID -> LayerRole.EYELID;
            case SCLERA -> LayerRole.DECORATION;
        });
    }

    int order(FaceScriptModel.Part part) {
        return part.settings().order.orElseGet(() -> switch (part.kind()) {
            case SCLERA -> sideOrder(part.variant(), 10, 11);
            case PUPIL -> sideOrder(part.variant(), 15, 16);
            case EYELID -> "Upper".equals(part.variant()) ? 20 : 22;
            case BROW -> sideOrder(part.variant(), 30, 31);
        });
    }

    int glassesOrder() {
        return 40;
    }

    int glassesOrder(FaceScriptModel.Settings settings) {
        return settings.order.orElseGet(this::glassesOrder);
    }

    IntRect glassesRect(FaceScriptModel.Settings settings) {
        return settings.textureArea.orElseGet(() -> {
            FaceScriptModel.Size size = settings.size.orElse(new FaceScriptModel.Size(8, 8));
            return new IntRect(0, 0, size.width(), size.height());
        });
    }

    String glassesTexture(String root, FaceScriptModel.Settings settings) {
        return texturePath(root, settings.texture.orElse("glasses"));
    }

    IntRect rect(FaceScriptModel.Part part) {
        return part.settings().textureArea.orElseGet(() -> {
            FaceScriptModel.Size size = part.settings().size.orElse(defaultSize(part.kind()));
            requirePositive(size.width(), size.height(), part.kind().name());
            return new IntRect(0, 0, size.width(), size.height());
        });
    }

    IntRect eyelidRect(FaceScriptModel.Part part, String side) {
        IntRect area = rect(part);
        if (area.width() % 2 != 0) {
            throw new IllegalArgumentException("Eyelid TextureArea width must be even.");
        }
        int half = area.width() / 2;
        int x = "left".equals(side) ? area.x() : area.x() + half;
        return new IntRect(x, area.y(), half, area.height());
    }

    Vec3 position(FaceScriptModel.Part part) {
        return part.settings().position.orElse(Vec3.ZERO);
    }

    Vec3 eyelidPosition(FaceScriptModel.Part part, String side) {
        Vec3 base = position(part);
        double offset = "left".equals(side) ? EYE_LEFT_X : EYE_RIGHT_X;
        return new Vec3(base.x() + offset, base.y(), base.z());
    }

    String normalize(String value) {
        StringBuilder result = new StringBuilder();
        char previous = 0;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            appendNormalized(result, previous, current);
            previous = current;
        }
        return result.toString().replaceAll("_+", "_").replaceAll("^_|_$", "");
    }

    private void appendNormalized(StringBuilder result, char previous, char current) {
        if (current == '-' || current == ' ') {
            result.append('_');
            return;
        }
        if (Character.isUpperCase(current) && result.length() > 0 && Character.isLowerCase(previous)) {
            result.append('_');
        }
        result.append(Character.toLowerCase(current));
    }

    private FaceScriptModel.Size defaultSize(FaceScriptModel.PartKind kind) {
        return switch (kind) {
            case SCLERA -> new FaceScriptModel.Size(2, 2);
            case PUPIL -> new FaceScriptModel.Size(1, 2);
            case BROW -> new FaceScriptModel.Size(2, 1);
            case EYELID -> new FaceScriptModel.Size(8, 2);
        };
    }

    private void requirePositive(int width, int height, String label) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(label + " Size and TextureArea dimensions must be positive.");
        }
    }

    private int sideOrder(String variant, int left, int right) {
        return "Left".equals(variant) ? left : right;
    }

    private String side(String variant) {
        return lower(variant);
    }

    private String lower(String value) {
        return value.toLowerCase(java.util.Locale.ROOT);
    }
}
