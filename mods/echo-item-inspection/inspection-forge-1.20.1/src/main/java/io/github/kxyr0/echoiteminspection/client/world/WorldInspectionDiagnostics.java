package io.github.kxyr0.echoiteminspection.client.world;

import net.minecraft.Util;

public final class WorldInspectionDiagnostics {
    private static WorldInspectionDiagnostic current = WorldInspectionDiagnostic.ITEM_INSPECTION_IPLA_UNAVAILABLE;
    private static String detail = "IPLA not loaded";
    private static long lastLogged;

    private WorldInspectionDiagnostics() { }

    public static void record(WorldInspectionDiagnostic diagnostic, String message) {
        boolean changed = current != diagnostic;
        current = diagnostic;
        detail = message;
        long now = Util.getMillis();
        if (!changed || now - lastLogged < 2000) return;
        lastLogged = now;
        com.mojang.logging.LogUtils.getLogger().debug("{}: {}", diagnostic, message);
    }

    public static String status() { return current + ": " + detail; }
    public static WorldInspectionDiagnostic current() { return current; }
}
