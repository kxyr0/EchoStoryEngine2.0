package io.github.kxyr0.echoiteminspection.client.api;

import io.github.kxyr0.echoiteminspection.client.screen.ItemInspectionScreen;
import io.github.kxyr0.echoiteminspection.client.session.ItemInspectionReturnContext;
import io.github.kxyr0.echoiteminspection.client.session.ItemInspectionSession;
import io.github.kxyr0.echoiteminspection.forge.InspectionRuntime;
import io.github.kxyr0.echoiteminspection.model.ItemInspectionDefinition;
import java.util.Optional;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import io.github.kxyr0.echoiteminspection.client.world.ItemInspectionWorldTarget;

/** Small client presentation boundary; no synthetic input or inventory writes. */
public final class ItemInspectionClient {
    private ItemInspectionClient() { }

    public static boolean isInspectable(ItemStack stack) {
        return resolveInspection(stack).isPresent();
    }

    public static Optional<ItemInspectionDefinition> resolveInspection(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Optional.empty();
        var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? Optional.empty() : InspectionRuntime.catalog().resolve(id.toString());
    }

    public static boolean openInspection(ItemStack stack) {
        return open(stack, null);
    }

    /** The world runtime must revalidate the synchronized target before calling this boundary. */
    public static boolean openWorldInspection(ItemInspectionWorldTarget target) {
        if (target == null || Minecraft.getInstance().screen != null) return false;
        return open(target.snapshot().copyStack(), target);
    }

    private static boolean open(ItemStack stack, ItemInspectionWorldTarget target) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) throw new IllegalStateException("Open inspection on the client thread");
        if (minecraft.player == null || minecraft.level == null
                || minecraft.screen instanceof ItemInspectionScreen) return false;
        var definition = resolveInspection(stack);
        if (definition.isEmpty()) return false;
        var context = new ItemInspectionReturnContext(minecraft);
        if (!context.isValid(minecraft)) return false;
        var session = new ItemInspectionSession(definition.orElseThrow(), stack, context, target);
        minecraft.mouseHandler.releaseMouse();
        KeyMapping.releaseAll();
        minecraft.pushGuiLayer(new ItemInspectionScreen(session));
        return true;
    }
}
