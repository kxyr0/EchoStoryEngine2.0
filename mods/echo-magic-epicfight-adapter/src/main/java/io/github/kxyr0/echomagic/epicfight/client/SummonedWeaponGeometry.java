package io.github.kxyr0.echomagic.epicfight.client;

import io.github.kxyr0.echomagic.forge.api.summoned.SummonedWeaponData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.utils.math.OpenMatrix4f;

final class SummonedWeaponGeometry {
    private SummonedWeaponGeometry() { }

    static Shape weapon(Player player, OpenMatrix4f main, OpenMatrix4f off, ItemStack stack) {
        return switch (SummonedWeaponData.form(stack)) {
            case "spear" -> spear(main, stack);
            case "whip" -> whip(player, main, stack);
            case "gauntlets" -> gauntlets(main, off);
            case "blade" -> blade(main, stack);
            case "dagger" -> dagger(main, stack);
            default -> rapier(main, stack);
        };
    }

    private static Shape spear(OpenMatrix4f hand, ItemStack stack) {
        double length = SummonedWeaponData.bladeLength(stack);
        List<Path> core = List.of(line(hand, 0.0D, 0.10D, 0.28D, 0.0D, 0.10D, -length, 30));
        Vec3 left = point(hand, -0.31D, 0.10D, -length + 0.05D);
        Vec3 right = point(hand, 0.31D, 0.10D, -length + 0.05D);
        Vec3 tip = point(hand, 0.0D, 0.10D, -length - 0.66D);
        List<Vec3> edge = new ArrayList<>();
        append(edge, left, tip, 11); append(edge, tip, right, 11); append(edge, right, left, 9);
        return new Shape(core, List.of(path(edge)), List.of(), tip);
    }

    private static Shape whip(Player player, OpenMatrix4f hand, ItemStack stack) {
        double length = SummonedWeaponData.bladeLength(stack);
        Path straight = line(hand, 0.0D, 0.10D, -0.08D,
                0.0D, 0.10D, -0.08D - length * 2.175D, 58);
        return new Shape(List.of(straight), List.of(), List.of(), straight.points().get(57));
    }
    private static Shape gauntlets(OpenMatrix4f main, OpenMatrix4f off) {
        List<Path> shell = new ArrayList<>(); List<Path> knuckles = new ArrayList<>();
        gauntlet(shell, knuckles, main); if (off != null) gauntlet(shell, knuckles, off);
        return new Shape(shell, knuckles, List.of(), point(main, 0.0D, 0.08D, -0.38D));
    }

    private static void gauntlet(List<Path> shell, List<Path> knuckles, OpenMatrix4f hand) {
        for (int ring = 0; ring < 6; ring++) {
            List<Vec3> points = new ArrayList<>(); double z = 0.08D - ring * 0.08D;
            for (int corner = 0; corner <= 12; corner++) {
                double angle = corner * Math.PI / 6.0D;
                points.add(point(hand, Math.cos(angle) * 0.17D, 0.08D + Math.sin(angle) * 0.15D, z));
            }
            shell.add(path(points));
        }
        List<Vec3> fingers = new ArrayList<>();
        for (int finger = -2; finger <= 2; finger++) {
            fingers.add(point(hand, finger * 0.07D, 0.13D, -0.39D));
        }
        knuckles.add(path(fingers));
    }

    private static Shape blade(OpenMatrix4f hand, ItemStack stack) {
        double length = SummonedWeaponData.bladeLength(stack);
        List<Vec3> spine = new ArrayList<>(); List<Vec3> edge = new ArrayList<>();
        for (int index = 0; index <= 25; index++) {
            double t = index / 25.0D;
            spine.add(point(hand, 0.03D + 0.19D * t * t, 0.10D, -0.10D - length * t));
            edge.add(point(hand, -0.02D - 0.08D * Math.sin(t * Math.PI), 0.10D, -0.10D - length * t));
        }
        Vec3 tip = point(hand, 0.23D, 0.10D, -0.15D - length);
        spine.set(spine.size() - 1, tip); edge.set(edge.size() - 1, tip);
        Path guard = line(hand, -0.12D, 0.10D, -0.06D, 0.14D, 0.10D, -0.06D, 12);
        return new Shape(List.of(path(spine), path(edge)), List.of(guard), List.of(), tip);
    }

    private static Shape rapier(OpenMatrix4f hand, ItemStack stack) {
        double length = SummonedWeaponData.bladeLength(stack);
        List<Path> blade = List.of(line(hand, 0.0D, 0.10D, -0.12D, 0.0D, 0.10D, -0.12D - length, 24));
        Path guard = line(hand, -0.18D, 0.10D, -0.08D, 0.18D, 0.10D, -0.08D, 13);
        Vec3 tip = blade.get(0).points().get(blade.get(0).points().size() - 1);
        return new Shape(blade, List.of(guard), List.of(point(hand, 0.0D, 0.10D, 0.08D)), tip);
    }

    private static Shape dagger(OpenMatrix4f hand, ItemStack stack) {
        double length = SummonedWeaponData.bladeLength(stack);
        Path middle = line(hand, 0.0D, 0.10D, 0.02D, 0.0D, 0.10D, -length, 17);
        Vec3 left = point(hand, -0.13D, 0.10D, -0.08D);
        Vec3 right = point(hand, 0.13D, 0.10D, -0.08D);
        Vec3 tip = point(hand, 0.0D, 0.10D, -length - 0.15D);
        List<Vec3> edge = new ArrayList<>(); append(edge, left, tip, 12); append(edge, tip, right, 12);
        return new Shape(List.of(middle), List.of(path(edge)), List.of(), tip);
    }

    private static Path line(OpenMatrix4f hand, double ax, double ay, double az,
                             double bx, double by, double bz, int samples) {
        List<Vec3> points = new ArrayList<>();
        append(points, point(hand, ax, ay, az), point(hand, bx, by, bz), samples);
        return path(points);
    }

    private static void append(List<Vec3> out, Vec3 from, Vec3 to, int samples) {
        for (int index = 0; index < samples; index++) {
            Vec3 point = from.lerp(to, samples == 1 ? 1.0D : index / (double) (samples - 1));
            if (out.isEmpty() || out.get(out.size() - 1).distanceToSqr(point) > 1.0E-10D) out.add(point);
        }
    }

    private static Path path(List<Vec3> points) { return new Path(List.copyOf(points)); }
    private static Vec3 point(OpenMatrix4f hand, double x, double y, double z) {
        return OpenMatrix4f.transform(hand, new Vec3(x, y, z));
    }

    record Path(List<Vec3> points) { }
    record Shape(List<Path> corePaths, List<Path> accentPaths, List<Vec3> motes, Vec3 tip) {
        Shape { corePaths = List.copyOf(corePaths); accentPaths = List.copyOf(accentPaths); motes = List.copyOf(motes); }
    }
}
