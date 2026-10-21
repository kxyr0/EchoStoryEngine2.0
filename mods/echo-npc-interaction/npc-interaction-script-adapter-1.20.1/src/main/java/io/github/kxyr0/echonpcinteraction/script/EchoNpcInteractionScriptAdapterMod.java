package io.github.kxyr0.echonpcinteraction.script;

import io.github.kxyr0.echonpcinteraction.script.dialogue.DialogueInteractionWaits;
import io.github.kxyr0.echonpcinteraction.script.dialogue.NpcInteractionDialogueListener;
import io.github.kxyr0.echonpcinteraction.script.story.NpcInteractionStoryExtensionFactory;
import io.github.kxyr0.echonpcinteraction.script.trigger.NpcInteractionTriggerListener;
import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExtensionRegistrationEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(EchoNpcInteractionScriptAdapterMod.MOD_ID)
public final class EchoNpcInteractionScriptAdapterMod {
    public static final String MOD_ID = "echo_npc_interaction_script_adapter";

    private final DialogueInteractionWaits dialogueWaits = new DialogueInteractionWaits();
    private final NpcInteractionStoryExtensionFactory storyFactory = new NpcInteractionStoryExtensionFactory();

    public EchoNpcInteractionScriptAdapterMod() {
        MinecraftForge.EVENT_BUS.addListener(this::registerStoryExtension);
        MinecraftForge.EVENT_BUS.register(new NpcInteractionDialogueListener(dialogueWaits));
        MinecraftForge.EVENT_BUS.register(new NpcInteractionTriggerListener());
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
