package io.github.kxyr0.echoquests.forge.condition;

import io.github.kxyr0.echoquests.api.condition.QuestCondition;
import io.github.kxyr0.echoquests.api.condition.QuestConditionResolver;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class VanillaQuestConditionResolver implements QuestConditionResolver {
    private final Optional<ServerPlayer> player;

    public VanillaQuestConditionResolver(Optional<ServerPlayer> player) {
        this.player = player;
    }

    @Override
    public boolean test(QuestCondition condition) {
        if (player.isEmpty()) return false;
        return switch (condition.type()) {
            case "echo_quests:player_in_area" -> inArea(player.orElseThrow(), condition);
            case "echo_quests:player_has_item" -> hasItem(player.orElseThrow(), condition);
            default -> false;
        };
    }

    private boolean inArea(ServerPlayer value, QuestCondition condition) {
        try {
            if (!value.level().dimension().location().toString().equals(argument(condition, "dimension"))) {
                return false;
            }
            double x = number(condition, "x");
            double y = number(condition, "y");
            double z = number(condition, "z");
            double radius = number(condition, "radius");
            return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z) && Double.isFinite(radius)
                    && radius > 0.0D && value.distanceToSqr(x, y, z) <= radius * radius;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean hasItem(ServerPlayer value, QuestCondition condition) {
        try {
            ResourceLocation id = ResourceLocation.tryParse(argument(condition, "item"));
            if (id == null) return false;
            var item = ForgeRegistries.ITEMS.getValue(id);
            if (item == null) return false;
            int required = Integer.parseInt(argument(condition, "count"));
            if (required < 1) return false;
            int found = count(value.getInventory().items, item)
                    + count(value.getInventory().armor, item)
                    + count(value.getInventory().offhand, item);
            return found >= required;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private int count(List<ItemStack> stacks, net.minecraft.world.item.Item item) {
        return stacks.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    private String argument(QuestCondition condition, String name) {
        String value = condition.arguments().get(name);
        if (value == null) throw new IllegalArgumentException("Missing condition argument: " + name);
        return value;
    }

    private double number(QuestCondition condition, String name) {
        return Double.parseDouble(argument(condition, name));
    }
}
