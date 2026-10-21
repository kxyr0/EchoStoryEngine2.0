package io.github.kxyr0.echosavemigration.forge;

import com.mojang.brigadier.CommandDispatcher;
import io.github.kxyr0.echosavemigration.graph.MigrationPlanningException;
import io.github.kxyr0.echosavemigration.persistence.codec.BinarySaveCodec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;

final class SaveMigrationCommands {
    private SaveMigrationCommands() { }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher, ForgeMigrationCatalog catalog) {
        dispatcher.register(Commands.literal("echo_save").requires(source -> source.hasPermission(2))
                .then(Commands.literal("migration")
                        .then(Commands.literal("status")
                                .executes(context -> status(context.getSource(), catalog)))
                        .then(Commands.literal("history")
                                .executes(context -> history(context.getSource())))
                        .then(Commands.literal("dry-run")
                                .executes(context -> dryRun(context.getSource(), catalog)))));
    }

    private static int status(CommandSourceStack source, ForgeMigrationCatalog catalog) {
        Path save = savePath(source);
        if (!Files.isRegularFile(save)) {
            return failure(source, "Echo save is not present: " + save);
        }
        try {
            var snapshot = new BinarySaveCodec().read(save);
            source.sendSuccess(() -> Component.literal("Current save schema: " + snapshot.manifest().schema().value()
                    + "; runtime schema: " + catalog.currentSchema().value()), false);
            return 1;
        } catch (IOException exception) {
            return failure(source, "SAVE_MIGRATION_RESOURCE_NOT_FOUND: " + exception.getMessage());
        }
    }

    private static int dryRun(CommandSourceStack source, ForgeMigrationCatalog catalog) {
        Path save = savePath(source);
        if (!Files.isRegularFile(save)) return failure(source, "Echo save is not present: " + save);
        try {
            var snapshot = new BinarySaveCodec().read(save);
            var plan = catalog.graph().plan(snapshot.manifest().schema(), catalog.currentSchema());
            source.sendSuccess(() -> Component.literal("Dry run only; save will not be written."), false);
            source.sendSuccess(() -> Component.literal("Current save schema: " + plan.source().value()
                    + "; runtime schema: " + plan.target().value()), false);
            plan.steps().forEach(step -> source.sendSuccess(() -> Component.literal("Planned: "
                    + step.from().value() + " -> " + step.to().value() + " [" + step.id().value() + "]"), false));
            return 1;
        } catch (IOException | MigrationPlanningException exception) {
            return failure(source, exception.getMessage());
        }
    }

    private static int history(CommandSourceStack source) {
        Path save = savePath(source);
        if (!Files.isRegularFile(save)) return failure(source, "Echo save is not present: " + save);
        try {
            var receipts = new BinarySaveCodec().read(save).receipts();
            if (receipts.isEmpty()) source.sendSuccess(() -> Component.literal("No migrations applied."), false);
            receipts.forEach(receipt -> source.sendSuccess(() -> Component.literal(receipt.migrationId().value()
                    + ": " + receipt.from().value() + " -> " + receipt.to().value()
                    + " at " + receipt.appliedAt()), false));
            return 1;
        } catch (IOException exception) {
            return failure(source, "SAVE_MIGRATION_RESOURCE_NOT_FOUND: " + exception.getMessage());
        }
    }

    private static Path savePath(CommandSourceStack source) {
        return source.getServer().getWorldPath(LevelResource.ROOT).resolve("echo").resolve("save.bin");
    }

    private static int failure(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message));
        return 0;
    }
}
