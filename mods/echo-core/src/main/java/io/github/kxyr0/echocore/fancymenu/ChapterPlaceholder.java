package io.github.kxyr0.echocore.fancymenu;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import io.github.kxyr0.echocore.api.ChapterApi;
import io.github.kxyr0.echocore.api.ChapterResult.Code;
import io.github.kxyr0.echocore.api.ChapterSnapshot.State;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import net.minecraft.client.Minecraft;

public final class ChapterPlaceholder extends Placeholder {
    private final String field;
    public ChapterPlaceholder(String field) { super("echo_core_chapter_" + field); this.field = field; }
    @Override public boolean canRunAsync() { return false; }
    @Override public String getReplacementFor(DeserializedPlaceholderString input) {
        String id = input.values == null ? "chapter_1" : input.values.getOrDefault("chapter", "chapter_1");
        var state = ChapterApi.get().snapshot(id);
        return switch (field) {
            case "state" -> state.state().name();
            case "save_exists" -> Boolean.toString(state.saveExists());
            case "template_exists" -> Boolean.toString(state.templateExists());
            case "can_continue" -> Boolean.toString(state.state() == State.READY && Minecraft.getInstance().level == null
                    && Minecraft.getInstance().getSingleplayerServer() == null && Minecraft.getInstance().getConnection() == null);
            case "last_result" -> state.lastResult().code().name();
            case "last_error" -> state.lastResult().success() || state.lastResult().code() == Code.NONE ? "" : state.lastResult().detail();
            default -> "";
        };
    }
    @Override public List<String> getValueNames() { return List.of("chapter"); }
    @Override public String getDisplayName() { return "Echo Core: chapter " + field; }
    @Override public List<String> getDescription() { return List.of(); }
    @Override public String getCategory() { return "Echo Core"; }
    @Override public DeserializedPlaceholderString getDefaultPlaceholderString() {
        return new DeserializedPlaceholderString(getIdentifier(), new HashMap<>(Map.of("chapter", "chapter_1")), "");
    }
}
