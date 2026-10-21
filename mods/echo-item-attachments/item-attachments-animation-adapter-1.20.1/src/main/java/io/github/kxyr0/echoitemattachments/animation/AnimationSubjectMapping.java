package io.github.kxyr0.echoitemattachments.animation;

import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import io.github.kxyr0.echoitemattachments.api.identity.SubjectKind;

final class AnimationSubjectMapping {
    AnimationSubjectId animation(AttachmentSubjectRef subject) {
        return subject.kind() == SubjectKind.NPC
                ? new AnimationSubjectId(subject.value())
                : new AnimationSubjectId("echo_item_attachments_player:" + subject.value());
    }
}
