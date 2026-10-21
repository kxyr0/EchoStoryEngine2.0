package io.github.kxyr0.echothoughtspace.forge.client.render;

import io.github.kxyr0.echothoughtspace.api.ThoughtNodeId;
import io.github.kxyr0.echothoughtspace.core.model.ThoughtCanvasNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public final class ThoughtLabelCache {
    private final Map<ThoughtNodeId, Entry> entries = new HashMap<>();

    public List<FormattedCharSequence> lines(Font font, ThoughtCanvasNode node) {
        Entry current = entries.get(node.snapshot().id());
        long revision = node.snapshot().revision().value();
        if (current == null || current.revision != revision
                || !current.label.equals(node.snapshot().label().value())) {
            List<FormattedCharSequence> lines = font.split(
                    Component.literal(node.snapshot().label().value()), 210);
            current = new Entry(revision, node.snapshot().label().value(), List.copyOf(lines));
            entries.put(node.snapshot().id(), current);
        }
        return current.lines;
    }

    private record Entry(long revision, String label, List<FormattedCharSequence> lines) { }
}

