package io.github.kxyr0.echocore.fancymenu;

import de.keksuccino.fancymenu.customization.action.Action;
import io.github.kxyr0.echocore.client.ChapterMenuScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class OpenChapterMenuAction extends Action {
    public OpenChapterMenuAction() { super("echo_core_open_chapter_menu"); }
    @Override public boolean hasValue() { return false; }
    @Override public boolean canRunAsync() { return false; }
    @Override public void execute(String value) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (!(mc.screen instanceof ChapterMenuScreen)) mc.setScreen(new ChapterMenuScreen(mc.screen));
        });
    }
    @Override public Component getDisplayName() { return Component.literal("Echo Core: open chapter menu"); }
    @Override public Component getDescription() { return Component.empty(); }
    @Override public Component getValueDisplayName() { return Component.empty(); }
    @Override public String getValuePreset() { return ""; }
}
