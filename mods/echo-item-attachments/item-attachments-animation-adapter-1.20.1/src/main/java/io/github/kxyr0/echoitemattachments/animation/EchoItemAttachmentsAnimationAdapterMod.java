package io.github.kxyr0.echoitemattachments.animation;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(EchoItemAttachmentsAnimationAdapterMod.MOD_ID)
public final class EchoItemAttachmentsAnimationAdapterMod {
    public static final String MOD_ID = "echo_item_attachments_animation_adapter";

    public EchoItemAttachmentsAnimationAdapterMod() {
        ActivePlaybackBindings bindings = new ActivePlaybackBindings();
        AttachmentAnimationStarter starter = new AttachmentAnimationStarter(bindings);
        AttachmentMarkerListener markers = new AttachmentMarkerListener(bindings);
        AttachmentAnimationMonitor monitor = new AttachmentAnimationMonitor(bindings);
        AnimationSocketAnchorProvider anchors = new AnimationSocketAnchorProvider(bindings);
        MinecraftForge.EVENT_BUS.addListener(starter::start);
        MinecraftForge.EVENT_BUS.addListener(markers::marker);
        MinecraftForge.EVENT_BUS.addListener(monitor::tick);
        MinecraftForge.EVENT_BUS.addListener(anchors::sample);
    }
}
