package io.github.kxyr0.echocore.fancymenu;

import de.keksuccino.fancymenu.customization.action.Action;
import io.github.kxyr0.echocore.api.ChapterApi;
import net.minecraft.network.chat.Component;

public final class ChapterAction extends Action {
    private final String operation;
    public ChapterAction(String operation) { super("echo_core_chapter_" + operation); this.operation = operation; }
    @Override public boolean hasValue() { return true; }
    @Override public boolean canRunAsync() { return false; }
    @Override public void execute(String value) {
        String chapter = value == null || value.isBlank() ? "chapter_1" : value.trim();
        switch (operation) {
            case "start" -> ChapterApi.get().start(chapter);
            case "restart" -> ChapterApi.get().restart(chapter);
            default -> ChapterApi.get().continueChapter(chapter);
        }
    }
    @Override public Component getDisplayName() { return Component.literal("Echo Core: " + operation + " chapter"); }
    @Override public Component getDescription() { return Component.empty(); }
    @Override public Component getValueDisplayName() { return Component.literal("Chapter ID"); }
    @Override public String getValuePreset() { return "chapter_1"; }
}
