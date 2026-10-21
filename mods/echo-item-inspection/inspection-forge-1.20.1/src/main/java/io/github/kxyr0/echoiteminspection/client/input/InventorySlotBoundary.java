package io.github.kxyr0.echoiteminspection.client.input;

import java.lang.reflect.Method;
import java.util.Arrays;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

/** Conservative boundary: custom mouse/slot overrides retain ownership of their gestures. */
public final class InventorySlotBoundary {
    private static final Class<?>[] MOUSE_SIGNATURE = {double.class, double.class, int.class};
    private static final Class<?>[] SLOT_SIGNATURE = {Slot.class, int.class, int.class, ClickType.class};
    private static final ClassValue<Boolean> NORMAL = new ClassValue<>() {
        @Override protected Boolean computeValue(Class<?> type) {
            return normalHierarchy(type);
        }
    };

    private InventorySlotBoundary() { }

    public static boolean isNormal(AbstractContainerScreen<?> screen) {
        if (screen instanceof CreativeModeInventoryScreen) return false;
        if (screen.width < 379 && screen instanceof RecipeUpdateListener recipes
                && recipes.getRecipeBookComponent().isVisible()) return false;
        return NORMAL.get(screen.getClass());
    }

    public static Slot findSlot(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        double x = mouseX - screen.getGuiLeft();
        double y = mouseY - screen.getGuiTop();
        for (Slot slot : screen.getMenu().slots) {
            if (slot.isActive() && x >= slot.x - 1 && x < slot.x + 17
                    && y >= slot.y - 1 && y < slot.y + 17) return slot;
        }
        return null;
    }

    private static boolean normalHierarchy(Class<?> type) {
        for (Class<?> current = type; current != AbstractContainerScreen.class;
                current = current.getSuperclass()) {
            if (current == null) return false;
            if (current.getName().startsWith("net.minecraft.")) continue;
            for (Method method : current.getDeclaredMethods()) {
                if (customInteraction(method)) return false;
            }
        }
        return true;
    }

    private static boolean customInteraction(Method method) {
        Class<?>[] signature = method.getParameterTypes();
        return (method.getReturnType() == boolean.class && Arrays.equals(signature, MOUSE_SIGNATURE))
                || (method.getReturnType() == void.class && Arrays.equals(signature, SLOT_SIGNATURE));
    }
}
