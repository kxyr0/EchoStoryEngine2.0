package io.github.kxyr0.echodebug.core.render;

import io.github.kxyr0.echotrigger.api.authoring.catalog.AreaGeometrySnapshot;
import io.github.kxyr0.echotrigger.api.geometry.AreaShapeKind;
import io.github.kxyr0.echotrigger.api.geometry.Vector3;
import java.util.ArrayList;
import java.util.List;

final class TriggerFootprintSquares {
    private static final double SURFACE_OFFSET = 0.025D;
    private static final int MAXIMUM_SQUARES = 65_536;

    List<RenderLine> lines(AreaGeometrySnapshot geometry) {
        int minX = floor(geometry.min().X());
        int maxX = ceil(geometry.max().X());
        int minZ = floor(geometry.min().Z());
        int maxZ = ceil(geometry.max().Z());
        if ((long) (maxX - minX) * (maxZ - minZ) > MAXIMUM_SQUARES) {
            return outline(geometry, minX, maxX, minZ, maxZ);
        }
        double y = surfaceY(geometry);
        List<RenderLine> lines = new ArrayList<>();
        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                if (covers(geometry, x + 0.5D, z + 0.5D)) {
                    addSquare(lines, x, y, z);
                }
            }
        }
        return List.copyOf(lines);
    }

    private static boolean covers(AreaGeometrySnapshot geometry, double x, double z) {
        return switch (geometry.shapeKind()) {
            case BOX -> true;
            case SPHERE, CYLINDER -> radial(geometry, x, z);
            case POLYGON -> polygon(geometry.polygonPoints(), x, z);
        };
    }

    private static boolean radial(AreaGeometrySnapshot geometry, double x, double z) {
        double dx = x - geometry.center().X();
        double dz = z - geometry.center().Z();
        double radius = geometry.radius().orElse(0.0D);
        return dx * dx + dz * dz <= radius * radius;
    }

    private static boolean polygon(List<Vector3> points, double x, double z) {
        boolean inside = false;
        for (int current = 0, previous = points.size() - 1; current < points.size(); previous = current++) {
            Vector3 a = points.get(current);
            Vector3 b = points.get(previous);
            boolean crosses = (a.Z() > z) != (b.Z() > z);
            double atX = (b.X() - a.X()) * (z - a.Z()) / (b.Z() - a.Z()) + a.X();
            inside = crosses && x < atX ? !inside : inside;
        }
        return inside;
    }

    private static void addSquare(List<RenderLine> lines, double x, double y, double z) {
        Vector3 northWest = new Vector3(x, y, z);
        Vector3 northEast = new Vector3(x + 1.0D, y, z);
        Vector3 southEast = new Vector3(x + 1.0D, y, z + 1.0D);
        Vector3 southWest = new Vector3(x, y, z + 1.0D);
        lines.add(new RenderLine(northWest, northEast, "square"));
        lines.add(new RenderLine(northEast, southEast, "square"));
        lines.add(new RenderLine(southEast, southWest, "square"));
        lines.add(new RenderLine(southWest, northWest, "square"));
    }

    private static List<RenderLine> outline(
            AreaGeometrySnapshot geometry,
            int minX,
            int maxX,
            int minZ,
            int maxZ
    ) {
        List<RenderLine> lines = new ArrayList<>();
        double y = surfaceY(geometry);
        Vector3 northWest = new Vector3(minX, y, minZ);
        Vector3 northEast = new Vector3(maxX, y, minZ);
        Vector3 southEast = new Vector3(maxX, y, maxZ);
        Vector3 southWest = new Vector3(minX, y, maxZ);
        lines.add(new RenderLine(northWest, northEast, "outline"));
        lines.add(new RenderLine(northEast, southEast, "outline"));
        lines.add(new RenderLine(southEast, southWest, "outline"));
        lines.add(new RenderLine(southWest, northWest, "outline"));
        return List.copyOf(lines);
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private static int ceil(double value) {
        return (int) Math.ceil(value);
    }

    private static double surfaceY(AreaGeometrySnapshot geometry) {
        return floor(geometry.min().Y()) + 1.0D + SURFACE_OFFSET;
    }
}
