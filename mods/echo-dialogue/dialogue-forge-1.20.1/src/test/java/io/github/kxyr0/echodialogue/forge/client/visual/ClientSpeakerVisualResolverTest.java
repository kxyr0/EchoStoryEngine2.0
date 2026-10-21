package io.github.kxyr0.echodialogue.forge.client.visual;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualId;
import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualKind;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ClientSpeakerVisualResolverTest {
    @Test
    void skinHeadSearchesDialogueNpcPlayerAndDirectTextureRoots() {
        ResourceLocation[] locations = ClientSpeakerVisualResolver.textureLocations(
                SpeakerVisualKind.SKIN_HEAD, SpeakerVisualId.parse("echo_content_player:player/base"));

        assertEquals(ResourceLocation.fromNamespaceAndPath("echo_content_player",
                "textures/echo_dialogue/skins/player/base.png"), locations[0]);
        assertEquals(ResourceLocation.fromNamespaceAndPath("echo_content_player",
                "textures/echo_npc/skins/player/base.png"), locations[1]);
        assertEquals(ResourceLocation.fromNamespaceAndPath("echo_content_player",
                "textures/echo_player/skins/player/base.png"), locations[2]);
        assertEquals(ResourceLocation.fromNamespaceAndPath("echo_content_player",
                "textures/player/base.png"), locations[3]);
    }

    @Test
    void skinHeadCanResolveAuthoredTexturePathsDirectly() {
        ResourceLocation[] locations = ClientSpeakerVisualResolver.textureLocations(
                SpeakerVisualKind.SKIN_HEAD,
                SpeakerVisualId.parse("jdh:echo_wardrobe/skins/origin/john_outfit/john_outfit_1"));

        assertEquals(ResourceLocation.fromNamespaceAndPath("jdh",
                "textures/echo_wardrobe/skins/origin/john_outfit/john_outfit_1.png"), locations[3]);
    }

    @Test
    void portraitCanUseModernSkinSheetAsHeadSource() {
        assertTrue(ClientSpeakerVisualResolver.skinSheetCompatible(SpeakerVisualKind.PORTRAIT, 64, 64));
        assertTrue(ClientSpeakerVisualResolver.skinSheetCompatible(SpeakerVisualKind.SKIN_HEAD, 64, 64));
        assertFalse(ClientSpeakerVisualResolver.skinSheetCompatible(SpeakerVisualKind.PORTRAIT, 128, 128));
    }
}
