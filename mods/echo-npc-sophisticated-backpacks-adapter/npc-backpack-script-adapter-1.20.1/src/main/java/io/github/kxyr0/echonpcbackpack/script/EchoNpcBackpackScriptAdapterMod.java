package io.github.kxyr0.echonpcbackpack.script;

import io.github.kxyr0.echonpcbackpack.script.dialogue.DialogueBackpackWaits;
import io.github.kxyr0.echonpcbackpack.script.dialogue.NpcBackpackDialogueListener;
import io.github.kxyr0.echonpcbackpack.script.story.NpcBackpackStoryExtensionFactory;
import io.github.kxyr0.echonpcbackpack.script.trigger.NpcBackpackTriggerListener;
import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExtensionRegistrationEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Registers the canonical calls in every installed EchoScript host.
 */
@Mod(EchoNpcBackpackScriptAdapterMod.MOD_ID)
public final class EchoNpcBackpackScriptAdapterMod {
    public static final String MOD_ID = "echo_npc_sophisticated_backpacks_script_adapter";

    private final DialogueBackpackWaits dialogueWaits = new DialogueBackpackWaits();
    private final NpcBackpackStoryExtensionFactory storyFactory = new NpcBackpackStoryExtensionFactory();

    public EchoNpcBackpackScriptAdapterMod() {
        MinecraftForge.EVENT_BUS.addListener(this::registerStoryExtension);
        MinecraftForge.EVENT_BUS.register(new NpcBackpackDialogueListener(dialogueWaits));
        MinecraftForge.EVENT_BUS.register(new NpcBackpackTriggerListener());
        MinecraftForge.EVENT_BUS.addListener(this::tick);
        MinecraftForge.EVENT_BUS.addListener(this::serverStopping);
    }

    private void registerStoryExtension(EchoStoryExtensionRegistrationEvent event) {
        event.registrar().register(storyFactory);
    }

    private void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            storyFactory.tick(event.getServer());
            dialogueWaits.tick(event.getServer());
        }
    }

    private void serverStopping(ServerStoppingEvent event) {
        storyFactory.close(event.getServer());
        dialogueWaits.close(event.getServer());
    }
}
