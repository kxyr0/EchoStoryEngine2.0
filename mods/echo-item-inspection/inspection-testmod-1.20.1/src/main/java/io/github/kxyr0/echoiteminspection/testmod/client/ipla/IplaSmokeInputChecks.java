package io.github.kxyr0.echoiteminspection.testmod.client.ipla;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.kxyr0.echoiteminspection.client.input.WorldInspectionInput;
import io.github.kxyr0.echoiteminspection.client.render.WorldInspectionPromptRenderer;
import io.github.kxyr0.echoiteminspection.client.screen.ItemInspectionScreen;
import io.github.kxyr0.echoiteminspection.client.world.ItemInspectionSourceKind;
import io.github.kxyr0.echoiteminspection.client.world.WorldInspectionRuntime;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.client.ForgeHooksClient;
import org.lwjgl.glfw.GLFW;

final class IplaSmokeInputChecks {
    private final IplaSmokeEvidence evidence;

    IplaSmokeInputChecks(IplaSmokeEvidence evidence) { this.evidence = evidence; }

    static void press(int key) {
        var input = InputConstants.Type.KEYSYM.getOrCreate(key);
        KeyMapping.set(input, true);
        KeyMapping.click(input);
        ForgeHooksClient.onKeyInput(key, 0, GLFW.GLFW_PRESS, 0);
        KeyMapping.set(input, false);
        ForgeHooksClient.onKeyInput(key, 0, GLFW.GLFW_RELEASE, 0);
    }

    void openExact(Minecraft minecraft, int key) {
        WorldInspectionRuntime.refresh();
        evidence.check("worldTargetBeforePress", WorldInspectionRuntime.prompt().activeTarget() != null);
        evidence.check("promptUsesActualKey", WorldInspectionPromptRenderer.label().getString().contains(
                WorldInspectionInput.INSPECT.getTranslatedKeyMessage().getString()));
        press(key);
        evidence.check("worldKeyOpenedExistingViewer", minecraft.screen instanceof ItemInspectionScreen);
        var session = ((ItemInspectionScreen) minecraft.screen).session();
        evidence.check("worldSourceTyped", session.sourceKind() == ItemInspectionSourceKind.IPLA_WORLD);
        evidence.check("worldParentIsGameplay", session.returnContext().parent() == null);
        evidence.check("exactPlacedNbt", IplaSmokeFixture.token().save(new CompoundTag()).equals(
                session.snapshot().copyStack().save(new CompoundTag())));
        evidence.check("exactCustomModel", session.snapshot().copyStack().getTag().getInt("CustomModelData") == 19);
        evidence.check("exactGlint", session.snapshot().copyStack().hasFoil());
        evidence.check("exactDamage", session.snapshot().copyStack().getDamageValue() == 17);
        evidence.check("worldMetadataImmutable", session.worldTarget().orElseThrow().snapshot().fingerprint()
                .equals(session.snapshot().fingerprint()));
        evidence.check("inventoryClickQueueConsumedBeforeVanilla", !minecraft.options.keyInventory.consumeClick());
        var viewer = (ItemInspectionScreen) minecraft.screen;
        viewer.mouseClicked(100, 100, 0);
        viewer.mouseDragged(130, 110, 0, 30, 10);
        viewer.mouseReleased(130, 110, 0);
        viewer.mouseScrolled(130, 110, 2);
        evidence.check("worldViewerZoomWorks", session.transform().zoom() > 1);
    }

    void close(Minecraft minecraft) {
        ((ItemInspectionScreen) minecraft.screen).keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0);
        evidence.check("worldEscReturnsDirectlyToGameplay", minecraft.screen == null);
        evidence.check("worldMouseRegrabbed", minecraft.mouseHandler.isMouseGrabbed());
    }

    static void bind(int key) {
        WorldInspectionInput.INSPECT.setKey(InputConstants.Type.KEYSYM.getOrCreate(key));
        KeyMapping.resetMapping();
    }
}
