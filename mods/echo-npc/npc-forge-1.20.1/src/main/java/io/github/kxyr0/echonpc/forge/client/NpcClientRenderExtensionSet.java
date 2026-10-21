package io.github.kxyr0.echonpc.forge.client;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echonpc.forge.api.client.render.NpcClientRenderExtension;
import io.github.kxyr0.echonpc.forge.api.client.render.NpcClientRenderExtensionDescriptor;
import io.github.kxyr0.echonpc.forge.api.client.render.NpcClientRenderResult;
import io.github.kxyr0.echonpc.forge.api.client.render.NpcHeadRenderContext;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;

/**
 * Internal safety boundary for invoking public NPC render extensions.
 */
final class NpcClientRenderExtensionSet {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final List<Binding> bindings;
    private final Set<String> reportedFailures = new HashSet<>();

    NpcClientRenderExtensionSet(List<Binding> bindings) {
        this.bindings = List.copyOf(bindings);
    }

    boolean empty() {
        return bindings.isEmpty();
    }

    NpcClientRenderResult render(NpcHeadRenderContext context) {
        NpcClientRenderResult result = NpcClientRenderResult.NOT_HANDLED;
        for (Binding binding : bindings) {
            result = merge(result, renderOne(binding, context));
            if (result == NpcClientRenderResult.CANCELLED) {
                return result;
            }
        }
        return result;
    }

    private NpcClientRenderResult renderOne(Binding binding, NpcHeadRenderContext context) {
        try {
            return binding.extension().render(context);
        } catch (RuntimeException exception) {
            reportFailure(binding, exception);
            return NpcClientRenderResult.FAILED;
        }
    }

    private NpcClientRenderResult merge(NpcClientRenderResult current, NpcClientRenderResult next) {
        if (next == NpcClientRenderResult.CANCELLED
                || next == NpcClientRenderResult.REPLACED_DEFAULT_MODEL) {
            return next;
        }
        if (current == NpcClientRenderResult.NOT_HANDLED) {
            return next;
        }
        return current;
    }

    private void reportFailure(Binding binding, RuntimeException exception) {
        String id = binding.descriptor().id().toString();
        if (reportedFailures.add(id)) {
            LOGGER.warn("Echo NPC render extension failed: {}", id, exception);
        }
    }

    record Binding(NpcClientRenderExtensionDescriptor descriptor, NpcClientRenderExtension extension) {
    }
}
