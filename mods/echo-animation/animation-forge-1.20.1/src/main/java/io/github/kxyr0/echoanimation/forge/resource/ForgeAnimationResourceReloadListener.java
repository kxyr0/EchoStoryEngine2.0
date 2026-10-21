package io.github.kxyr0.echoanimation.forge.resource;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoanimation.api.diagnostic.AnimationDiagnostic;
import io.github.kxyr0.echoanimation.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echoanimation.echoscript.diagnostic.AnimationDiagnosticMessages;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

/**
 * Forge reload listener for Echo Animation data resources.
 */
public final class ForgeAnimationResourceReloadListener
        extends SimplePreparableReloadListener<AnimationCatalogCandidate> {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final AnimationResourceDiscovery discovery;
    private final AnimationResourceCandidateCompiler compiler;
    private final AnimationCatalogState catalogState;
    private final AnimationReloadChatNotifier chatNotifier;
    private final Supplier<Optional<MinecraftServer>> server;
    private final Path externalContentPackRoot;

    public ForgeAnimationResourceReloadListener(
            AnimationResourceDiscovery discovery,
            AnimationResourceCandidateCompiler compiler,
            AnimationCatalogState catalogState,
            Supplier<Optional<MinecraftServer>> server,
            Path externalContentPackRoot
    ) {
        this.discovery = discovery;
        this.compiler = compiler;
        this.catalogState = catalogState;
        this.chatNotifier = new AnimationReloadChatNotifier();
        this.server = server;
        this.externalContentPackRoot = externalContentPackRoot;
    }

    @Override
    public String getName() {
        return "echo_animation_resources";
    }

    @Override
    protected AnimationCatalogCandidate prepare(ResourceManager manager, ProfilerFiller profiler) {
        try {
            return compiler.compile(discovery.discover(manager, externalContentPackRoot));
        } catch (RuntimeException exception) {
            AnimationDiagnostic diagnostic = reloadFailure(exception);
            LOGGER.debug("Echo Animation resource reload exception details.", exception);
            return new AnimationCatalogCandidate(catalogState.current(), List.of(diagnostic));
        }
    }

    @Override
    protected void apply(AnimationCatalogCandidate candidate, ResourceManager manager, ProfilerFiller profiler) {
        catalogState.install(candidate);
        LOGGER.info("Echo Animation catalog installed: face_rigs={}, face_poses={}, face_animations={}, "
                        + "body_rigs={}, body_clips={}, animations={}, animation_profiles={}, diagnostics={}",
                candidate.resourceSet().faceRigs().size(),
                candidate.resourceSet().facePoses().size(),
                candidate.resourceSet().facialAnimations().size(),
                candidate.resourceSet().animationRigs().size(),
                candidate.resourceSet().animationClips().size(),
                candidate.resourceSet().animations().size(),
                candidate.resourceSet().animationProfiles().size(),
                candidate.diagnostics().size());
        for (AnimationDiagnostic diagnostic : candidate.diagnostics()) {
            if (diagnostic.error()) {
                LOGGER.error("[{}] {}", diagnostic.code(), diagnostic.message());
            } else {
                LOGGER.warn("[{}] {}", diagnostic.code(), diagnostic.message());
            }
        }
        chatNotifier.notifyErrors(server.get(), candidate.diagnostics());
        if (!candidate.valid()) {
            LOGGER.warn("[EANIM-FORGE-CATALOG-RETAINED] "
                    + "Предыдущий каталог Echo Animation сохранен из-за ошибок перезагрузки.");
        }
    }

    private AnimationDiagnostic reloadFailure(RuntimeException exception) {
        AnimationDiagnostic diagnostic = new AnimationDiagnostic("EANIM-FORGE-RELOAD",
                DiagnosticSeverity.ERROR, "Перезагрузка каталога анимаций не выполнена: "
                + exception.getClass().getSimpleName() + ": " + exception.getMessage());
        return AnimationDiagnosticMessages.localized(diagnostic);
    }
}
