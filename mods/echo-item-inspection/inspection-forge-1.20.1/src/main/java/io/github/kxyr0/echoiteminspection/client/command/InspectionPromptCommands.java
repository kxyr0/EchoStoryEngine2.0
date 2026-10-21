package io.github.kxyr0.echoiteminspection.client.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.kxyr0.echoiteminspection.forge.InspectionClientConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

/** Local presentation settings: apply immediately and persist in the client config. */
public final class InspectionPromptCommands {
    private InspectionPromptCommands() { }

    public static LiteralArgumentBuilder<CommandSourceStack> tree() {
        return Commands.literal("prompt").executes(context -> status(context.getSource()))
                .then(Commands.literal("scale").then(Commands.argument("value", DoubleArgumentType.doubleArg(0.5, 3))
                        .executes(context -> {
                            InspectionClientConfig.PROMPT_SCALE.set(DoubleArgumentType.getDouble(context, "value"));
                            return save(context.getSource());
                        })))
                .then(integerSetting("width", 0, 1200, InspectionClientConfig.PROMPT_MIN_WIDTH))
                .then(integerSetting("height", 24, 120, InspectionClientConfig.PROMPT_HEIGHT))
                .then(Commands.literal("reset").executes(context -> {
                    InspectionClientConfig.PROMPT_SCALE.set(1.0);
                    InspectionClientConfig.PROMPT_MIN_WIDTH.set(0);
                    InspectionClientConfig.PROMPT_HEIGHT.set(36);
                    return save(context.getSource());
                }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> integerSetting(String name, int minimum,
            int maximum, ForgeConfigSpec.IntValue setting) {
        return Commands.literal(name).then(Commands.argument("value", IntegerArgumentType.integer(minimum, maximum))
                .executes(context -> {
                    setting.set(IntegerArgumentType.getInteger(context, "value"));
                    return save(context.getSource());
                }));
    }

    private static int save(CommandSourceStack source) {
        InspectionClientConfig.SPEC.save();
        return status(source);
    }

    private static int status(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("echo_item_inspection.prompt.settings",
                InspectionClientConfig.PROMPT_SCALE.get(), InspectionClientConfig.PROMPT_MIN_WIDTH.get(),
                InspectionClientConfig.PROMPT_HEIGHT.get()), false);
        return 1;
    }
}
