package io.github.kxyr0.echonpc.forge.client.attachment;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echonpc.forge.api.client.attachment.NpcBodyAttachmentRenderContext;
import io.github.kxyr0.echonpc.forge.api.client.attachment.NpcBodyAttachmentRenderExtension;
import io.github.kxyr0.echonpc.forge.api.client.render.NpcClientRenderExtensionDescriptor;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;

/**
 * Internal failure boundary for public body attachment extensions.
 */
public final class NpcBodyAttachmentExtensionSet {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final List<Binding> bindings;
    private final Set<String> reportedFailures = new HashSet<>();

    public NpcBodyAttachmentExtensionSet(List<Binding> bindings) {
        this.bindings = List.copyOf(bindings);

        LOGGER.info(
                "ENPC-BODY-ATTACHMENT-SET-CREATED bindings={}",
                this.bindings.size());

        for (Binding binding : this.bindings) {
            LOGGER.info(
                    "ENPC-BODY-ATTACHMENT-BINDING id={} extension={}",
                    binding.descriptor().id(),
                    binding.extension().getClass().getName());
        }
    }

    public boolean empty() {
        return bindings.isEmpty();
    }

    public void render(NpcBodyAttachmentRenderContext context) {
        LOGGER.info(
                "ENPC-BODY-ATTACHMENT-RENDER actor={} form={} visible={} bindings={}",
                context.actorId(),
                context.formId(),
                context.visible(),
                bindings.size());

        for (Binding binding : bindings) {
            String id = binding.descriptor().id().toString();
            String extensionClass =
                    binding.extension().getClass().getName();

            LOGGER.info(
                    "ENPC-BODY-ATTACHMENT-CALL actor={} id={} extension={}",
                    context.actorId(),
                    id,
                    extensionClass);

            try {
                var result = binding.extension().render(context);

                LOGGER.info(
                        "ENPC-BODY-ATTACHMENT-RESULT actor={} id={} result={}",
                        context.actorId(),
                        id,
                        result);
            } catch (RuntimeException exception) {
                LOGGER.error(
                        "ENPC-BODY-ATTACHMENT-FAILED actor={} id={} extension={}",
                        context.actorId(),
                        id,
                        extensionClass,
                        exception);

                reportedFailures.add(id);
            }
        }

    }

    public record Binding(
            NpcClientRenderExtensionDescriptor descriptor,
            NpcBodyAttachmentRenderExtension extension) {
    }
}
