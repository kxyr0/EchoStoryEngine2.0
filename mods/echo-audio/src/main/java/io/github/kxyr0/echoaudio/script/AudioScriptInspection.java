package io.github.kxyr0.echoaudio.script;

import io.github.kxyr0.echoaudio.api.AudioCue;
import io.github.kxyr0.echoaudio.api.AudioFailure;
import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echoscript.api.syntax.CompilationUnitSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.ClassSectionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.BlockStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.SectionStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ExpressionStatementSyntax;
import java.util.function.BiConsumer;

/** Descriptor traversal of existing EchoScript syntax; command semantics remain in the one shared binder. */
public final class AudioScriptInspection {
    public void inspect(CompilationUnitSyntax unit, BiConsumer<AudioCue, SourceSpan> references,
            BiConsumer<AudioFailure, SourceSpan> problems) {
        unit.classDeclarations().forEach(type -> type.members().forEach(member -> {
            if (member instanceof MethodSyntax method) block(method.body(), references, problems);
            if (member instanceof ClassSectionSyntax section) block(section.body(), references, problems);
        }));
    }
    private void block(BlockStatementSyntax block, BiConsumer<AudioCue, SourceSpan> references,
            BiConsumer<AudioFailure, SourceSpan> problems) {
        for (var statement : block.statements()) {
            if (statement instanceof SectionStatementSyntax section) block(section.body(), references, problems);
            if (statement instanceof BlockStatementSyntax nested) block(nested, references, problems);
            if (!(statement instanceof ExpressionStatementSyntax expression)
                    || !(expression.expression() instanceof InvocationExpressionSyntax call)
                    || !new AudioCommandBinder().accepts(call)) continue;
            try { references.accept(new AudioCommandBinder().bind(call), call.span()); }
            catch (AudioFailure failure) { problems.accept(failure, call.span()); }
        }
    }
}
