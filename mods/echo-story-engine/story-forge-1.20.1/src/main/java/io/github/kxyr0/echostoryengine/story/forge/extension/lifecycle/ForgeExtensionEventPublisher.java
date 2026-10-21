package io.github.kxyr0.echostoryengine.story.forge.extension.lifecycle;

import io.github.kxyr0.echostoryengine.story.core.port.DiagnosticSink;
import io.github.kxyr0.echostoryengine.story.core.port.StoryClock;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionApiVersion;
import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExtensionRegistrationEvent;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionFactory;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.ForgeRuntimeDiagnostics;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

/**
 * Posts the bounded server-scoped extension registration event.
 */
public final class ForgeExtensionEventPublisher {
    private final DiagnosticSink diagnosticSink;
    private final StoryClock clock;
    private final ForgeRuntimeDiagnostics diagnostics;

    public ForgeExtensionEventPublisher(
            DiagnosticSink diagnosticSink,
            StoryClock clock,
            ForgeRuntimeDiagnostics diagnostics) {
        this.diagnosticSink = Objects.requireNonNull(diagnosticSink, "diagnosticSink");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }

    public List<ForgeStoryExtensionFactory> collect(MinecraftServer server) {
        ExtensionRegistrationCollector collector = new ExtensionRegistrationCollector();
        diagnosticSink.accept(diagnostics.extensionRegistrationOpened(clock.now()));
        MinecraftForge.EVENT_BUS.post(new EchoStoryExtensionRegistrationEvent(
                server,
                collector,
                StoryExtensionApiVersion.current()));
        List<ForgeStoryExtensionFactory> factories = collector.closeAndList();
        diagnosticSink.accept(diagnostics.extensionRegistrationClosed(factories.size(), clock.now()));
        return factories;
    }
}
