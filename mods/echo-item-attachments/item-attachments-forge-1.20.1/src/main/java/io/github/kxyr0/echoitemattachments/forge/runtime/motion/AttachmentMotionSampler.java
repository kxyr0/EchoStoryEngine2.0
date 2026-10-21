package io.github.kxyr0.echoitemattachments.forge.runtime.motion;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentId;
import io.github.kxyr0.echoitemattachments.api.model.ItemCustodyState;
import io.github.kxyr0.echoitemattachments.core.model.CustodyRecord;
import io.github.kxyr0.echoitemattachments.core.motion.HandVelocityWindow;
import io.github.kxyr0.echoitemattachments.core.motion.Vector3d;
import io.github.kxyr0.echoitemattachments.forge.api.anchor.AttachmentAnchorBridge;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;

public final class AttachmentMotionSampler {
    private final Map<AttachmentId, HandVelocityWindow> windows = new LinkedHashMap<>();

    public void sample(MinecraftServer server, Collection<CustodyRecord> records) {
        records.stream().filter(record -> record.state() == ItemCustodyState.ATTACHED_TO_SUBJECT
                || record.state() == ItemCustodyState.RELEASING).forEach(record ->
                AttachmentAnchorBridge.sample(server, record.subject(), record.point()).ifPresent(anchor ->
                        windows.computeIfAbsent(record.attachmentId(), ignored -> new HandVelocityWindow())
                                .sample(anchor.serverTick(), new Vector3d(anchor.position().x,
                                        anchor.position().y, anchor.position().z))));
    }

    public Optional<Vector3d> velocity(AttachmentId id) {
        return Optional.ofNullable(windows.get(id)).flatMap(window -> window.velocity(2.5D));
    }
}
