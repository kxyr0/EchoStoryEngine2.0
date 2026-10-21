package io.github.kxyr0.echoitemattachments.script;

import io.github.kxyr0.echoitemattachments.script.story.ItemAttachmentStoryExtensionFactory;
import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExtensionRegistrationEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(EchoItemAttachmentsScriptAdapterMod.MOD_ID)
public final class EchoItemAttachmentsScriptAdapterMod {
    public static final String MOD_ID = "echo_item_attachments_script_adapter";
    private final ItemAttachmentStoryExtensionFactory story = new ItemAttachmentStoryExtensionFactory();

    public EchoItemAttachmentsScriptAdapterMod() {
        MinecraftForge.EVENT_BUS.addListener(this::register);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
        MinecraftForge.EVENT_BUS.addListener(this::stopping);
    }

    private void register(EchoStoryExtensionRegistrationEvent event) {
        event.registrar().register(story);
    }

    private void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) story.tick(event.getServer());
    }

    private void stopping(ServerStoppingEvent event) {
        story.close(event.getServer());
    }
}
