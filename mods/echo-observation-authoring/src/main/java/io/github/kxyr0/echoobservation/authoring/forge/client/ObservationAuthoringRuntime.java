package io.github.kxyr0.echoobservation.authoring.forge.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.kxyr0.echoobservation.api.client.ObservationAuthoringReloadEvent;
import io.github.kxyr0.echoobservation.api.client.ObservationClientLookupEvent;
import io.github.kxyr0.echoobservation.api.client.ObservationClientView;
import io.github.kxyr0.echoobservation.api.client.ObservationClientScreenPoint;
import io.github.kxyr0.echoobservation.api.identity.ObservationId;
import io.github.kxyr0.echoobservation.api.identity.ObservationResourceId;
import io.github.kxyr0.echoobservation.api.model.ObservationAnchor;
import io.github.kxyr0.echoobservation.api.model.ObservationDefinition;
import io.github.kxyr0.echoobservation.api.runtime.ObservationServiceBridge;
import io.github.kxyr0.echoobservation.authoring.core.ObservationAtomicWriter;
import io.github.kxyr0.echoobservation.authoring.core.ObservationSourcePatch;
import io.github.kxyr0.echoobservation.authoring.core.ObservationSourcePath;
import io.github.kxyr0.echoobservation.authoring.forge.client.selection.ObservationAuthoringSelection;
import io.github.kxyr0.echoobservation.authoring.forge.client.picker.ObservationPickerItem;
import io.github.kxyr0.echoobservation.authoring.forge.client.picker.ObservationPickerScreen;
import io.github.kxyr0.echoobservation.authoring.forge.config.ObservationAuthoringConfig;
import io.github.kxyr0.echoobservation.forge.network.ObservationNetwork;
import io.github.kxyr0.echoobservation.forge.network.ObservationPackets;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.glfw.GLFW;

public final class ObservationAuthoringRuntime {
    private final ObservationAuthoringSelection selection = new ObservationAuthoringSelection();
    private final ObservationRaycast raycast = new ObservationRaycast();
    private final ObservationSourcePatch patch = new ObservationSourcePatch();
    private final ObservationAtomicWriter writer = new ObservationAtomicWriter();
    private Optional<ObservationAnchor> preview = Optional.empty();
    private Optional<ObservationId> pending = Optional.empty();
    private boolean advanceAfterReload;
    private boolean active;
    private ObservationResourceId requestedActivation;
    private String status = "F6: authoring disabled";

    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!ObservationAuthoringConfig.enabled()) { disable("disabled by config"); return; }
        if (ObservationAuthoringKeys.TOGGLE.consumeClick()) toggle(minecraft);
        if (!usable(minecraft)) { preview = Optional.empty(); return; }
        preview = raycast.cast(minecraft);
        Optional<ObservationClientView> view = view();
        if (view.isEmpty()) { status = "Observation runtime unavailable"; return; }
        selection.definition(view.orElseThrow());
        while (ObservationAuthoringKeys.PICKER.consumeClick()) openPicker(minecraft, view.orElseThrow());
        activateSelection(minecraft, view.orElseThrow());
        boolean resources = shiftDown(minecraft);
        while (ObservationAuthoringKeys.PREVIOUS.consumeClick()) cycle(view.orElseThrow(), -1, resources);
        while (ObservationAuthoringKeys.NEXT.consumeClick()) cycle(view.orElseThrow(), 1, resources);
        while (ObservationAuthoringKeys.PLACE.consumeClick()) place(minecraft, view.orElseThrow());
        while (ObservationAuthoringKeys.CLEAR.consumeClick()) clear(minecraft, view.orElseThrow());
    }

    public void accept(ObservationAuthoringReloadEvent event) {
        if (pending.isEmpty() || !pending.orElseThrow().equals(event.observationId())) return;
        status = event.successful() ? "Reload confirmed, catalog r" + event.catalogRevision()
                : "Reload rejected: " + event.message();
        pending = Optional.empty();
        if (event.successful() && advanceAfterReload) view().ifPresent(value -> {
            if (!selection.advanceUnplacedInResource(value)) status = "ALL OBSERVATIONS PLACED.";
        });
        advanceAfterReload = false;
    }

    public boolean active() { return active; }
    public String status() { return status; }
    public Optional<ObservationAnchor> preview() { return preview; }
    public Optional<ObservationDefinition> selected() { return view().flatMap(selection::definition); }
    public Optional<ObservationClientScreenPoint> selectedScreen() {
        return view().flatMap(client -> selection.definition(client)
                .flatMap(value -> value.anchor().flatMap(client::project)));
    }
    public Optional<ObservationClientScreenPoint> previewScreen() {
        return view().flatMap(client -> preview.flatMap(client::project));
    }
    public List<ObservationDefinition> definitions() {
        return view().map(value -> value.catalog().resources().stream()
                .flatMap(resource -> resource.observations().stream()).toList()).orElse(List.of());
    }

    private void openPicker(Minecraft minecraft, ObservationClientView view) {
        if (pending.isPresent()) { status = "Waiting for reload confirmation"; return; }
        List<ObservationPickerItem> items = definitions().stream().map(ObservationPickerItem::from).toList();
        String current = selection.selectedId().map(Object::toString).orElse("");
        minecraft.setScreen(new ObservationPickerScreen(items, current, id -> {
            selection.select(id);
            status = "Selected from picker: " + id;
        }));
    }

    private void toggle(Minecraft minecraft) {
        if (minecraft.getSingleplayerServer() == null || minecraft.level == null) {
            disable("authoring requires an integrated server"); return;
        }
        active = !active;
        status = active ? "Authoring enabled" : "Authoring disabled";
        if (!active) { preview = Optional.empty(); requestedActivation = null; }
    }

    private boolean usable(Minecraft minecraft) {
        return active && minecraft.level != null && minecraft.player != null && minecraft.screen == null
                && minecraft.getSingleplayerServer() != null;
    }

    private void cycle(ObservationClientView view, int direction, boolean resources) {
        if (pending.isPresent()) return;
        if (resources) selection.cycleResource(view, direction);
        else selection.cycleObservation(view, direction);
        status = resources ? "Selected resource" : "Selected observation";
    }

    private void place(Minecraft minecraft, ObservationClientView view) {
        if (pending.isPresent()) { status = "Waiting for reload confirmation"; return; }
        Optional<ObservationDefinition> selected = selection.definition(view);
        if (selected.isEmpty()) { status = "No Observation declarations"; return; }
        if (preview.isEmpty()) { status = "Raycast miss: source unchanged"; return; }
        updateSource(minecraft, selected.orElseThrow(), preview.orElseThrow(), true);
    }

    private void clear(Minecraft minecraft, ObservationClientView view) {
        if (pending.isPresent()) { status = "Waiting for reload confirmation"; return; }
        Optional<ObservationDefinition> selected = selection.definition(view);
        if (selected.isEmpty()) { status = "No Observation declarations"; return; }
        updateSource(minecraft, selected.orElseThrow(), null, false);
    }

    private void updateSource(Minecraft minecraft, ObservationDefinition definition,
            ObservationAnchor anchor, boolean placing) {
        var path = new ObservationSourcePath(minecraft.gameDirectory.toPath())
                .resolve(definition.source().sourceId());
        if (path.isEmpty() || !Files.isRegularFile(path.orElseThrow())) {
            status = "Source file is not editable: " + definition.source().sourceId(); return;
        }
        try {
            String source = Files.readString(path.orElseThrow(), StandardCharsets.UTF_8);
            var result = placing ? patch.place(source, definition, anchor) : patch.clear(source, definition);
            if (result.candidate().isEmpty()) { status = result.message(); return; }
            if (result.candidate().orElseThrow().equals(source)) { status = result.message(); return; }
            writer.write(path.orElseThrow(), result.candidate().orElseThrow());
            pending = Optional.of(definition.id());
            advanceAfterReload = placing;
            status = "Source committed; reloading…";
            ObservationNetwork.sendToServer(new ObservationPackets.ServerboundReload(definition.id().toString()));
        } catch (IOException | RuntimeException exception) {
            status = "Authoring failed: " + exception.getMessage();
        }
    }

    private Optional<ObservationClientView> view() {
        ObservationClientLookupEvent event = new ObservationClientLookupEvent();
        MinecraftForge.EVENT_BUS.post(event);
        return event.view();
    }

    private void activateSelection(Minecraft minecraft, ObservationClientView view) {
        ObservationResourceId resource = selection.definition(view)
                .map(value -> value.id().resource()).orElse(null);
        if (resource == null || resource.equals(requestedActivation)) return;
        var server = minecraft.getSingleplayerServer();
        if (server == null || minecraft.player == null) return;
        requestedActivation = resource;
        var playerId = minecraft.player.getUUID();
        server.execute(() -> ObservationServiceBridge.lookup(server)
                .ifPresent(service -> service.activate(playerId, resource)));
    }

    private boolean shiftDown(Minecraft minecraft) {
        long window = minecraft.getWindow().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private void disable(String message) {
        active = false; preview = Optional.empty(); pending = Optional.empty(); requestedActivation = null;
        status = message;
    }
}
