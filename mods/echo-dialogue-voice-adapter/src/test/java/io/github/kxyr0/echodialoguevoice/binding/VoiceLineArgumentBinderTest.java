package io.github.kxyr0.echodialoguevoice.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.api.source.SourcePosition;
import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.DecimalLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class VoiceLineArgumentBinderTest {
    private static final SourceSpan SPAN = new SourceSpan(new SourceId("test.es"), 0, 0,
            new SourcePosition(0, 1, 1), new SourcePosition(0, 1, 1));
    private final VoiceLineArgumentBinder binder = new VoiceLineArgumentBinder();

    @Test
    void exactVoiceDslBindsStemAndVolumeCues() {
        InvocationExpressionSyntax voice = new InvocationExpressionSyntax(new NameExpressionSyntax("Voice", SPAN),
                List.of(new ArgumentSyntax(Optional.empty(), new StringLiteralExpressionSyntax("john_002", SPAN),
                                SPAN),
                        new ArgumentSyntax(Optional.of("volume"),
                                new DecimalLiteralExpressionSyntax(new BigDecimal("0.8"), SPAN), SPAN)), SPAN);

        var result = binder.bind(voice);

        assertTrue(result.issues().isEmpty());
        assertEquals("john_002", result.cues().get(0).value());
        assertEquals("0.8", result.cues().get(1).value());
    }

    @Test
    void extensionNeverBecomesRuntimeIdentity() {
        InvocationExpressionSyntax voice = new InvocationExpressionSyntax(new NameExpressionSyntax("Voice", SPAN),
                List.of(new ArgumentSyntax(Optional.empty(), new StringLiteralExpressionSyntax("john_001.mp3", SPAN),
                        SPAN)), SPAN);

        assertTrue(binder.bind(voice).issues().stream()
                .anyMatch(issue -> issue.code().equals("VOICE_ASSET_FILENAME_INVALID")));
    }

    @Test
    void volumeOutsideRangeIsRejected() {
        InvocationExpressionSyntax voice = new InvocationExpressionSyntax(new NameExpressionSyntax("Voice", SPAN),
                List.of(new ArgumentSyntax(Optional.empty(), new StringLiteralExpressionSyntax("john", SPAN), SPAN),
                        new ArgumentSyntax(Optional.of("volume"),
                                new DecimalLiteralExpressionSyntax(new BigDecimal("1.1"), SPAN), SPAN)), SPAN);

        assertTrue(binder.bind(voice).issues().stream()
                .anyMatch(issue -> issue.code().equals("VOICE_DSL_VOLUME_RANGE")));
    }
}
