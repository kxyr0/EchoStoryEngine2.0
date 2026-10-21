package io.github.kxyr0.echocore.fancymenu;

import de.keksuccino.fancymenu.customization.action.ActionRegistry;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderRegistry;

public final class FancyMenuBridge {
    private FancyMenuBridge() { }
    public static void register() {
        ActionRegistry.register(new OpenChapterMenuAction());
        for (String action : new String[] {"start", "continue", "restart"}) ActionRegistry.register(new ChapterAction(action));
        for (String field : new String[] {"state", "save_exists", "template_exists", "can_continue", "last_result", "last_error"}) {
            PlaceholderRegistry.register(new ChapterPlaceholder(field));
        }
    }
}
