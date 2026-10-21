package io.github.kxyr0.echobackgroundnpc.echoscript.binding;

import io.github.kxyr0.echobackgroundnpc.api.conversation.AmbientDialogue;
import io.github.kxyr0.echobackgroundnpc.api.conversation.AmbientLine;
import io.github.kxyr0.echobackgroundnpc.api.conversation.AmbientQuestOffer;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.AmbientDialogueId;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundQuestId;
import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.ClassSectionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ExpressionStatementSyntax;
import java.util.Optional;

public final class AmbientDialogueResourceBinder {
    private final BackgroundCallReader calls = new BackgroundCallReader();

    public AmbientDialogue bind(ClassSyntax resource, BackgroundResourceContext context, long revision) {
        if (!resource.baseType().name().text().equals("AmbientDialogueResource")) {
            throw new IllegalArgumentException("Expected AmbientDialogueResource.");
        }
        var lines = calls.calls(resource, "Line").stream().map(call -> new AmbientLine(
                calls.string(call, 0).orElseThrow(() -> failure("Line speaker is required.")),
                calls.string(call, 1).orElseThrow(() -> failure("Line text is required.")),
                calls.nestedVoice(call), calls.source(call.span()))).toList();
        return new AmbientDialogue(new AmbientDialogueId(context.resourceId()), lines, completion(resource), revision);
    }

    private Optional<AmbientQuestOffer> completion(ClassSyntax resource) {
        var sections = calls.sections(resource, "OnComplete");
        if (sections.size() > 1) throw failure("OnComplete declared more than once.");
        return sections.stream().findFirst().map(this::quest);
    }

    private AmbientQuestOffer quest(ClassSectionSyntax section) {
        if (section.body().statements().size() != 1
                || !(section.body().statements().get(0) instanceof ExpressionStatementSyntax statement)) {
            throw failure("OnComplete must contain exactly one StartQuest call.");
        }
        var call = calls.statement(statement)
                .orElseThrow(() -> failure("OnComplete must contain a StartQuest call."));
        if (!calls.name(call).equals("StartQuest")) throw failure("OnComplete supports only StartQuest.");
        String questId = calls.string(call, 0).orElseThrow(() -> failure("StartQuest quest ID is required."));
        double range = calls.namedNumber(call, "range").orElse(AmbientQuestOffer.DEFAULT_MAXIMUM_DISTANCE);
        return new AmbientQuestOffer(new BackgroundQuestId(questId), range, calls.source(call.span()));
    }

    private IllegalArgumentException failure(String message) { return new IllegalArgumentException(message); }
}
