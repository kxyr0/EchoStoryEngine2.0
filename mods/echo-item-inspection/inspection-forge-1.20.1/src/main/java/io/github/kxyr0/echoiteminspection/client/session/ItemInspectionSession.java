package io.github.kxyr0.echoiteminspection.client.session;

import io.github.kxyr0.echoiteminspection.api.ItemStackSnapshot;
import io.github.kxyr0.echoiteminspection.math.ItemInspectionTransform;
import io.github.kxyr0.echoiteminspection.model.ItemInspectionDefinition;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;
import io.github.kxyr0.echoiteminspection.client.world.ItemInspectionWorldTarget;
import io.github.kxyr0.echoiteminspection.client.world.ItemInspectionSourceKind;
import java.util.Optional;

/** Pins both the definition revision and the concrete stack at the instant of opening. */
public final class ItemInspectionSession {
    public enum State { OPEN, CLOSED }

    private final UUID id = UUID.randomUUID();
    private final ItemInspectionDefinition definition;
    private final ItemStackSnapshot snapshot;
    private final ItemInspectionReturnContext returnContext;
    private final ItemInspectionTransform transform;
    private final ItemInspectionWorldTarget worldTarget;
    private State state = State.OPEN;

    public ItemInspectionSession(ItemInspectionDefinition definition, ItemStack stack,
            ItemInspectionReturnContext returnContext) {
        this(definition, stack, returnContext, null);
    }

    public ItemInspectionSession(ItemInspectionDefinition definition, ItemStack stack,
            ItemInspectionReturnContext returnContext, ItemInspectionWorldTarget worldTarget) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.snapshot = new ItemStackSnapshot(stack);
        this.returnContext = Objects.requireNonNull(returnContext, "returnContext");
        this.transform = new ItemInspectionTransform(definition.view());
        this.worldTarget = worldTarget;
    }

    public UUID id() {
        return id;
    }

    public ItemInspectionDefinition definition() {
        return definition;
    }

    public ItemStackSnapshot snapshot() {
        return snapshot;
    }

    public ItemInspectionReturnContext returnContext() {
        return returnContext;
    }

    public ItemInspectionTransform transform() {
        return transform;
    }

    public State state() {
        return state;
    }

    public ItemInspectionSourceKind sourceKind() {
        return worldTarget == null ? ItemInspectionSourceKind.INVENTORY : worldTarget.sourceKind();
    }

    public Optional<ItemInspectionWorldTarget> worldTarget() { return Optional.ofNullable(worldTarget); }

    public void close() {
        state = State.CLOSED;
    }
}
