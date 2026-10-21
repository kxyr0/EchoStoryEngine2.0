package io.github.kxyr0.echoiteminspection.client.integration.ipla;

import io.github.kxyr0.echoiteminspection.client.world.WorldInspectionDiagnostic;
import io.github.kxyr0.echoiteminspection.client.world.WorldInspectionDiagnostics;
import io.github.kxyr0.echoiteminspection.client.world.WorldInspectionRuntime;
import net.minecraftforge.fml.ModList;

public final class IplaInspectionIntegration {
    private IplaInspectionIntegration() { }

    public static void install() {
        var container = ModList.get().getModContainerById("ipla");
        if (container.isEmpty()) return;
        String version = container.orElseThrow().getModInfo().getVersion().toString();
        if (!version.equals("6.3.1")) {
            WorldInspectionDiagnostics.record(WorldInspectionDiagnostic.ITEM_INSPECTION_IPLA_VERSION_UNSUPPORTED, version);
            return;
        }
        installSupported();
    }

    private static void installSupported() {
        var resolver = new IplaInspectionTargetResolver();
        WorldInspectionRuntime.install(resolver::resolve, resolver::resolveFresh, resolver::clear);
        com.mojang.logging.LogUtils.getLogger().info("IPLA 6.3.1 world inspection enabled (read-only placed stacks)");
    }
}
