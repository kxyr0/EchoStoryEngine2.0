package io.github.kxyr0.echotrigger.api.geometry;

public interface AreaShape {
    AreaShapeKind kind();

    Vector3 min();

    Vector3 max();

    Vector3 center();

    boolean contains(Vector3 point);

    default Vector3 size() {
        return max().subtract(min());
    }

    default String stableText() {
        return kind() + "|" + min().stableText() + "|" + max().stableText();
    }
}
