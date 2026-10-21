package io.github.kxyr0.echoanimation.forge.client.reload;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoanimation.api.diagnostic.AnimationDiagnostic;
import io.github.kxyr0.echoanimation.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echoanimation.echoscript.diagnostic.AnimationDiagnosticMessages;
import io.github.kxyr0.echoanimation.forge.resource.AnimationCatalogCandidate;
import io.github.kxyr0.echoanimation.forge.resource.AnimationCatalogState;
import io.github.kxyr0.echoanimation.forge.resource.AnimationResourceCandidateCompiler;
import io.github.kxyr0.echoanimation.forge.resource.AnimationResourceDiscovery;
import java.nio.file.Path;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

/**
 * Reloads Echo Animation data without forcing Minecraft's texture/resource-pack reload path.
 */
public final class AnimationClientCatalogHotReloader {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final AnimationCatalogState catalogState;
    private final AnimationResourceDiscovery discovery;
    private final AnimationResourceCandidateCompiler compiler;
    private final Path externalContentPackRoot;

    public AnimationClientCatalogHotReloader(
            AnimationCatalogState catalogState,
            AnimationResourceDiscovery discovery,
            AnimationResourceCandidateCompiler compiler,
            Path externalContentPackRoot
    ) {
        this.catalogState = Objects.requireNonNull(catalogState, "catalogState");
        this.discovery = Objects.requireNonNull(discovery, "discovery");
        this.compiler = Objects.requireNonNull(compiler, "compiler");
        this.externalContentPackRoot = externalContentPackRoot;
    }

    public void reload() {
        try {
            ResourceManager manager = resourceManager();
            AnimationCatalogCandidate candidate = compiler.compile(discovery.discover(
                    manager, externalContentPackRoot));
            catalogState.install(candidate);
            LOGGER.info("Echo Animation hot catalog reload: face_rigs={}, face_animations={}, body_clips={}, "
                            + "animations={}, diagnostics={}",
                    candidate.resourceSet().faceRigs().size(),
                    candidate.resourceSet().facialAnimations().size(),
                    candidate.resourceSet().animationClips().size(),
                    candidate.resourceSet().animations().size(),
                    candidate.diagnostics().size());
            candidate.diagnostics().forEach(this::logDiagnostic);
        } catch (RuntimeException exception) {
            AnimationDiagnostic diagnostic = reloadFailure(exception);
            catalogState.install(new AnimationCatalogCandidate(catalogState.current(), java.util.List.of(diagnostic)));
            logDiagnostic(diagnostic);
            LOGGER.debug("Echo Animation hot reload exception details.", exception);
        }
    }

    private ResourceManager resourceManager() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getSingleplayerServer() != null) {
            return minecraft.getSingleplayerServer().getResourceManager();
        }
        return minecraft.getResourceManager();
    }

    private void logDiagnostic(AnimationDiagnostic diagnostic) {
        if (diagnostic.error()) {
            LOGGER.error("[{}] {}", diagnostic.code(), diagnostic.message());
        } else {
            LOGGER.warn("[{}] {}", diagnostic.code(), diagnostic.message());
        }
    }

    private AnimationDiagnostic reloadFailure(RuntimeException exception) {
        AnimationDiagnostic diagnostic = new AnimationDiagnostic("EANIM-FORGE-HOT-RELOAD",
                DiagnosticSeverity.ERROR, "Горячая перезагрузка каталога анимаций не выполнена: "
                + exception.getClass().getSimpleName() + ": " + exception.getMessage());
        return AnimationDiagnosticMessages.localized(diagnostic);
    }
}
