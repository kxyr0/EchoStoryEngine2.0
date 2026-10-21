package io.github.kxyr0.echostoryengine.story.forge.environment.resource;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.BlockPropertyDef;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.EnvironmentChangeDef;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.EnvironmentResourceDef;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.SetBlockChangeDef;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;

final class ForgeEnvironmentResourceValidator {
    private final ForgeEnvironmentResourceDiagnostics diagnostics = new ForgeEnvironmentResourceDiagnostics();

    List<StoryDiagnostic> validate(EnvironmentResourceDef resource, Instant occurredAt) {
        List<StoryDiagnostic> values = new ArrayList<>();
        for (EnvironmentChangeDef change : changes(resource)) {
            if (change instanceof SetBlockChangeDef setBlock) {
                validateSetBlock(resource, setBlock, occurredAt, values);
            }
        }
        return List.copyOf(values);
    }

    private void validateSetBlock(
            EnvironmentResourceDef resource,
            SetBlockChangeDef change,
            Instant occurredAt,
            List<StoryDiagnostic> values) {
        Block block = block(change);
        if (block == null) {
            values.add(diagnostics.unknownBlock(blockFailure(resource, change), occurredAt));
            return;
        }
        validateProperties(new ValidationContext(resource, change, occurredAt, values), block);
        validateNbt(resource, change, occurredAt, values);
    }

    private void validateProperties(
            ValidationContext context,
            Block block) {
        for (BlockPropertyDef property : context.change().properties()) {
            Property<?> blockProperty = block.defaultBlockState().getBlock().getStateDefinition()
                    .getProperty(property.name());
            if (blockProperty == null) {
                context.values().add(diagnostics.unknownProperty(propertyFailure(
                        context.resource(), context.change(), property), context.occurredAt()));
            } else if (blockProperty.getValue(property.value()).isEmpty()) {
                context.values().add(diagnostics.invalidPropertyValue(propertyFailure(
                        context.resource(), context.change(), property), context.occurredAt()));
            }
        }
    }

    private void validateNbt(
            EnvironmentResourceDef resource,
            SetBlockChangeDef change,
            Instant occurredAt,
            List<StoryDiagnostic> values) {
        if (change.blockEntityNbt().isEmpty()) {
            return;
        }
        try {
            TagParser.parseTag(change.blockEntityNbt().orElseThrow());
        } catch (CommandSyntaxException exception) {
            values.add(diagnostics.invalidNbt(new ForgeEnvironmentResourceDiagnostics.NbtFailure(
                    resource.id().asString(), change.blockId().asString(),
                    exception.getClass().getSimpleName(), change.location()), occurredAt));
        }
    }

    private Block block(SetBlockChangeDef change) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                change.blockId().namespace(), change.blockId().path());
        return ForgeRegistries.BLOCKS.getValue(id);
    }

    private List<EnvironmentChangeDef> changes(EnvironmentResourceDef resource) {
        return resource.states().stream()
                .flatMap(state -> state.changes().stream())
                .toList();
    }

    private ForgeEnvironmentResourceDiagnostics.BlockFailure blockFailure(
            EnvironmentResourceDef resource,
            SetBlockChangeDef change) {
        return new ForgeEnvironmentResourceDiagnostics.BlockFailure(
                resource.id().asString(), change.blockId().asString(), change.location());
    }

    private ForgeEnvironmentResourceDiagnostics.PropertyFailure propertyFailure(
            EnvironmentResourceDef resource,
            SetBlockChangeDef change,
            BlockPropertyDef property) {
        return new ForgeEnvironmentResourceDiagnostics.PropertyFailure(
                resource.id().asString(), change.blockId().asString(),
                property.name(), property.value(), property.location());
    }

    private record ValidationContext(
            EnvironmentResourceDef resource,
            SetBlockChangeDef change,
            Instant occurredAt,
            List<StoryDiagnostic> values) {
    }
}
