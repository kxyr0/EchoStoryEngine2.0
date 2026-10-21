package io.github.kxyr0.echowardrobe.forge.command;

import com.mojang.brigadier.context.CommandContext;
import io.github.kxyr0.echowardrobe.api.identity.WearableId;
import io.github.kxyr0.echowardrobe.api.outfit.OutfitVariantId;
import io.github.kxyr0.echowardrobe.api.profile.WardrobeProfileId;
import io.github.kxyr0.echowardrobe.core.coverage.WardrobeCoverageAnalyzer;
import io.github.kxyr0.echowardrobe.core.resolution.MissingOutfitSkeleton;
import io.github.kxyr0.echowardrobe.forge.ForgeWardrobeRuntimeState;
import io.github.kxyr0.echowardrobe.forge.content.ForgeWardrobeCatalogLoader;
import java.util.Objects;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

final class WardrobeCommandActions {
    private final ForgeWardrobeRuntimeState state;
    private final ForgeWardrobeCatalogLoader loader;
    private final WardrobeCoverageAnalyzer coverage = new WardrobeCoverageAnalyzer();
    private final MissingOutfitSkeleton skeleton = new MissingOutfitSkeleton();

    WardrobeCommandActions(ForgeWardrobeRuntimeState state, ForgeWardrobeCatalogLoader loader) {
        this.state = Objects.requireNonNull(state, "state");
        this.loader = Objects.requireNonNull(loader, "loader");
    }

    int status(CommandContext<CommandSourceStack> context) {
        String message = "Echo Wardrobe: catalogRevision=" + state.catalog().revision().value()
                + ", profiles=" + state.catalog().profiles().definitions().size()
                + ", wearables=" + state.catalog().wearables().count()
                + ", outfits=" + state.catalog().outfits().definitions().size()
                + ", diagnostics=" + state.diagnostics().size();
        return say(context, message, 1);
    }

    int inspect(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        state.observe(player.getUUID(), io.github.kxyr0.echowardrobe.forge.equipment.ForgeEquipmentSnapshots
                .currentStatic(player));
        var outfit = state.outfit(player.getUUID());
        String message = outfit.map(value -> "OutfitKey=" + value.outfitKey().serialize()
                + ", variant=" + value.outfitVariantId().map(Object::toString).orElse("-")
                + ", WorldSkin=" + value.worldSkin()
                + ", FaceSkin=" + value.faceSkin()
                + ", appearanceRevision=" + value.appearanceRevision())
                .orElse("No wardrobe appearance observed.");
        return say(context, message, 1);
    }

    int listWearables(CommandContext<CommandSourceStack> context) {
        state.catalog().wearables().definitions().forEach(definition ->
                line(context, definition.id() + " profile=" + definition.profileId()
                        + " slot=" + definition.slot() + " status=" + definition.status()));
        return state.catalog().wearables().count();
    }

    int listOutfits(CommandContext<CommandSourceStack> context) {
        WardrobeProfileId profile = optionalProfile(context);
        var variants = profile == null ? state.catalog().outfits().definitions()
                : state.catalog().outfits().byProfile(profile);
        variants.forEach(variant -> line(context, variant.id() + " key=" + variant.key().serialize()
                + " status=" + variant.status() + " world=" + variant.worldSkin()
                + " face=" + variant.faceSkin()));
        return variants.size();
    }

    int inspectWearable(CommandContext<CommandSourceStack> context) {
        WearableId id = WearableId.parse(ResourceLocationArgument.getId(context, "wearable").toString());
        String message = state.catalog().wearables().find(id)
                .map(definition -> definition.toString())
                .orElse("Wearable not found: " + id);
        return say(context, message, 1);
    }

    int inspectOutfit(CommandContext<CommandSourceStack> context) {
        OutfitVariantId id = OutfitVariantId.parse(ResourceLocationArgument.getId(context, "outfit").toString());
        String message = state.catalog().outfits().find(id)
                .map(variant -> variant.id() + " " + variant.key().serialize()
                        + " world=" + variant.worldSkin() + " face=" + variant.faceSkin())
                .orElse("Outfit not found: " + id);
        return say(context, message, 1);
    }

    int coverage(CommandContext<CommandSourceStack> context) {
        WardrobeProfileId profile = profile(context, "profile");
        var report = coverage.analyze(profile, state.catalog().wearables(), state.catalog().outfits());
        String message = "Coverage " + profile + ": total=" + report.totalExpected()
                + ", mapped=" + report.mapped()
                + ", missing=" + report.missing().size()
                + ", duplicateKeys=" + report.duplicateKeys()
                + ", unused=" + report.unusedVariants();
        return say(context, message, report.missing().size());
    }

    int missing(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        state.observe(player.getUUID(), io.github.kxyr0.echowardrobe.forge.equipment.ForgeEquipmentSnapshots
                .currentStatic(player));
        String message = state.outfit(player.getUUID())
                .map(outfit -> skeleton.source(outfit.outfitKey()))
                .orElse("No current OutfitKey.");
        return say(context, message, 1);
    }

    int reload(CommandContext<CommandSourceStack> context) {
        var result = state.acceptCatalog(loader.load(), "command");
        String message = "Echo Wardrobe reload: successful=" + result.successful()
                + ", catalogRevision=" + state.catalog().revision().value()
                + ", profiles=" + state.catalog().profiles().definitions().size()
                + ", wearables=" + state.catalog().wearables().count()
                + ", outfits=" + state.catalog().outfits().definitions().size()
                + ", diagnostics=" + result.diagnostics().size();
        return say(context, message, result.successful() ? 1 : 0);
    }

    int validate(CommandContext<CommandSourceStack> context) {
        return say(context, "Echo Wardrobe validate: diagnostics=" + state.diagnostics().size(), 1);
    }

    int diagnostics(CommandContext<CommandSourceStack> context) {
        state.diagnostics().forEach(diagnostic ->
                line(context, diagnostic.severity() + " " + diagnostic.code() + " " + diagnostic.message()));
        return state.diagnostics().size();
    }

    private WardrobeProfileId optionalProfile(CommandContext<CommandSourceStack> context) {
        try {
            return profile(context, "profile");
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private WardrobeProfileId profile(CommandContext<CommandSourceStack> context, String name) {
        return WardrobeProfileId.parse(ResourceLocationArgument.getId(context, name).toString());
    }

    private int say(CommandContext<CommandSourceStack> context, String message, int result) {
        line(context, message);
        return result;
    }

    private void line(CommandContext<CommandSourceStack> context, String message) {
        context.getSource().sendSuccess(() -> Component.literal(message), false);
    }
}
