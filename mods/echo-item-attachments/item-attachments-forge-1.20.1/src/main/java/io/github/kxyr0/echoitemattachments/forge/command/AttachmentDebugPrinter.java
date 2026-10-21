package io.github.kxyr0.echoitemattachments.forge.command;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import io.github.kxyr0.echoitemattachments.forge.api.anchor.AttachmentAnchorBridge;
import io.github.kxyr0.echoitemattachments.forge.runtime.ServerAttachmentHost;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

final class AttachmentDebugPrinter {
    List<String> inspect(ServerAttachmentHost host, AttachmentSubjectRef subject) {
        List<String> lines = new ArrayList<>();
        host.records().stream().filter(record -> record.subject().equals(subject)).forEach(record -> lines.add(
                "payload=" + record.attachmentId() + " item=" + record.itemId() + " count=" + record.count()
                        + " nbt=" + fingerprint(record.stackTag()) + " custody=" + record.custodyId()
                        + " state=" + record.state() + " hand=" + record.point()
                        + " source=" + record.sourceEntityId().orElse("-")
                        + " release=" + record.releaseId().map(Object::toString).orElse("-")
                        + " revision=" + record.revision()));
        host.operations().stream().filter(operation -> operation.subject().equals(subject)).forEach(operation -> {
            String marker = host.markerGate().binding(operation.commandId()).map(binding ->
                    binding.playbackId() + "/" + binding.playbackRevision() + "/" + binding.phase()
                            + "/" + binding.markerId()).orElse("immediate/approach");
            lines.add("transition=" + operation.commandId() + " owner=" + operation.ownerId()
                    + " hand=" + operation.point() + " marker=" + marker);
        });
        if (lines.isEmpty()) lines.add("No attachment custody or transition for " + subject);
        return lines;
    }

    List<String> anchors(ServerAttachmentHost host, AttachmentSubjectRef subject) {
        List<String> lines = new ArrayList<>();
        for (AttachmentPointId point : List.of(AttachmentPointId.LEFT_HAND, AttachmentPointId.RIGHT_HAND)) {
            AttachmentAnchorBridge.sample(host.server(), subject, point).ifPresentOrElse(anchor -> lines.add(
                    point + " pos=" + anchor.position() + " forward=" + anchor.forward()
                            + " up=" + anchor.up() + " tick=" + anchor.serverTick()),
                    () -> lines.add(point + " anchor unavailable"));
        }
        return lines;
    }

    private String fingerprint(byte[] bytes) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes);
            return HexFormat.of().formatHex(hash, 0, 8);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
