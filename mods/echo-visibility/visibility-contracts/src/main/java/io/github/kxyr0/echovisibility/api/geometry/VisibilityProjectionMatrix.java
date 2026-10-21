package io.github.kxyr0.echovisibility.api.geometry;

import java.util.Arrays;

public final class VisibilityProjectionMatrix {
    private final double[] values;

    public VisibilityProjectionMatrix(double[] columnMajorValues) {
        if (columnMajorValues == null || columnMajorValues.length != 16) {
            throw new IllegalArgumentException("Projection matrix must contain 16 values.");
        }
        if (Arrays.stream(columnMajorValues).anyMatch(value -> !Double.isFinite(value))) {
            throw new IllegalArgumentException("Projection matrix values must be finite.");
        }
        values = columnMajorValues.clone();
    }

    public double value(int column, int row) {
        if (column < 0 || column > 3 || row < 0 || row > 3) {
            throw new IndexOutOfBoundsException("Matrix index must be in 0..3.");
        }
        return values[column * 4 + row];
    }

    public double[] values() {
        return values.clone();
    }

    public double verticalFovDegrees() {
        return Math.toDegrees(2.0D * Math.atan(1.0D / Math.abs(value(1, 1))));
    }

    public double aspectRatio() {
        return Math.abs(value(1, 1) / value(0, 0));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof VisibilityProjectionMatrix matrix && Arrays.equals(values, matrix.values);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(values);
    }
}
