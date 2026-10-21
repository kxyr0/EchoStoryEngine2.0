package io.github.kxyr0.echoanimation.api.math;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable 4x4 transform matrix using row-major logical indexing and column-vector multiplication.
 */
public final class Matrix4Transform {
    private static final int SIZE = 16;
    public static final Matrix4Transform IDENTITY = fromRowMajor(
            1.0D, 0.0D, 0.0D, 0.0D,
            0.0D, 1.0D, 0.0D, 0.0D,
            0.0D, 0.0D, 1.0D, 0.0D,
            0.0D, 0.0D, 0.0D, 1.0D);

    private final double[] values;

    private Matrix4Transform(double[] values) {
        this.values = checkedCopy(values);
    }

    public static Matrix4Transform fromRowMajor(double... values) {
        return new Matrix4Transform(values);
    }

    public static Matrix4Transform fromColumnMajor(double... values) {
        double[] source = checkedCopy(values);
        double[] rowMajor = new double[SIZE];
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                rowMajor[index(row, column)] = source[index(column, row)];
            }
        }
        return new Matrix4Transform(rowMajor);
    }

    public static Matrix4Transform fromEuler(EulerTransform transform) {
        Objects.requireNonNull(transform, "transform");
        Vec3 pivot = transform.pivot();
        return translation(transform.translation())
                .multiply(translation(pivot))
                .multiply(rotationZ(transform.rotationDegrees().z()))
                .multiply(rotationY(transform.rotationDegrees().y()))
                .multiply(rotationX(transform.rotationDegrees().x()))
                .multiply(scale(transform.scale()))
                .multiply(translation(new Vec3(-pivot.x(), -pivot.y(), -pivot.z())));
    }

    public static Matrix4Transform lerp(Matrix4Transform left, Matrix4Transform right, double alpha) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (!Double.isFinite(alpha) || alpha < 0.0D || alpha > 1.0D) {
            throw new IllegalArgumentException("alpha must be finite in range 0..1.");
        }
        double[] result = new double[SIZE];
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                double a = left.value(row, column);
                double b = right.value(row, column);
                result[index(row, column)] = a + (b - a) * alpha;
            }
        }
        return new Matrix4Transform(result);
    }

    public Matrix4Transform multiply(Matrix4Transform right) {
        Objects.requireNonNull(right, "right");
        double[] result = new double[SIZE];
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                result[index(row, column)] = dot(right, row, column);
            }
        }
        return new Matrix4Transform(result);
    }

    public Vec3 transformPoint(Vec3 point) {
        Objects.requireNonNull(point, "point");
        double x = value(0, 0) * point.x() + value(0, 1) * point.y()
                + value(0, 2) * point.z() + value(0, 3);
        double y = value(1, 0) * point.x() + value(1, 1) * point.y()
                + value(1, 2) * point.z() + value(1, 3);
        double z = value(2, 0) * point.x() + value(2, 1) * point.y()
                + value(2, 2) * point.z() + value(2, 3);
        double w = value(3, 0) * point.x() + value(3, 1) * point.y()
                + value(3, 2) * point.z() + value(3, 3);
        if (Math.abs(w) < 1.0E-12D) {
            throw new IllegalStateException("Matrix transformed point to w=0.");
        }
        return new Vec3(x / w, y / w, z / w);
    }

    public double value(int row, int column) {
        requireCoordinate(row, "row");
        requireCoordinate(column, "column");
        return values[index(row, column)];
    }

    public float[] columnMajorFloats() {
        float[] result = new float[SIZE];
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                result[index(column, row)] = (float) value(row, column);
            }
        }
        return result;
    }

    private double dot(Matrix4Transform right, int row, int column) {
        double result = 0.0D;
        for (int inner = 0; inner < 4; inner++) {
            result += value(row, inner) * right.value(inner, column);
        }
        return result;
    }

    private static Matrix4Transform translation(Vec3 value) {
        return fromRowMajor(
                1.0D, 0.0D, 0.0D, value.x(),
                0.0D, 1.0D, 0.0D, value.y(),
                0.0D, 0.0D, 1.0D, value.z(),
                0.0D, 0.0D, 0.0D, 1.0D);
    }

    private static Matrix4Transform scale(Vec3 value) {
        return fromRowMajor(
                value.x(), 0.0D, 0.0D, 0.0D,
                0.0D, value.y(), 0.0D, 0.0D,
                0.0D, 0.0D, value.z(), 0.0D,
                0.0D, 0.0D, 0.0D, 1.0D);
    }

    private static Matrix4Transform rotationX(double degrees) {
        double radians = Math.toRadians(degrees);
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        return fromRowMajor(
                1.0D, 0.0D, 0.0D, 0.0D,
                0.0D, cosine, -sine, 0.0D,
                0.0D, sine, cosine, 0.0D,
                0.0D, 0.0D, 0.0D, 1.0D);
    }

    private static Matrix4Transform rotationY(double degrees) {
        double radians = Math.toRadians(degrees);
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        return fromRowMajor(
                cosine, 0.0D, sine, 0.0D,
                0.0D, 1.0D, 0.0D, 0.0D,
                -sine, 0.0D, cosine, 0.0D,
                0.0D, 0.0D, 0.0D, 1.0D);
    }

    private static Matrix4Transform rotationZ(double degrees) {
        double radians = Math.toRadians(degrees);
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        return fromRowMajor(
                cosine, -sine, 0.0D, 0.0D,
                sine, cosine, 0.0D, 0.0D,
                0.0D, 0.0D, 1.0D, 0.0D,
                0.0D, 0.0D, 0.0D, 1.0D);
    }

    private static double[] checkedCopy(double[] values) {
        Objects.requireNonNull(values, "values");
        if (values.length != SIZE) {
            throw new IllegalArgumentException("Matrix4Transform requires exactly 16 values.");
        }
        double[] copy = values.clone();
        for (double value : copy) {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("Matrix4Transform values must be finite.");
            }
        }
        return copy;
    }

    private static void requireCoordinate(int value, String name) {
        if (value < 0 || value >= 4) {
            throw new IndexOutOfBoundsException(name + " must be between 0 and 3.");
        }
    }

    private static int index(int row, int column) {
        return row * 4 + column;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof Matrix4Transform matrix && Arrays.equals(values, matrix.values);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(values);
    }

    @Override
    public String toString() {
        return "Matrix4Transform" + Arrays.toString(values);
    }
}
