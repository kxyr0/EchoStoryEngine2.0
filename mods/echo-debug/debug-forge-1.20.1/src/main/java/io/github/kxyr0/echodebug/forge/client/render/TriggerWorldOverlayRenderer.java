package io.github.kxyr0.echodebug.forge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kxyr0.echodebug.core.render.TriggerAreaRenderPlan;
import io.github.kxyr0.echodebug.core.render.TriggerAreaRenderPlanner;
import io.github.kxyr0.echodebug.core.selection.TriggerBlockPoint;
import io.github.kxyr0.echodebug.core.selection.TriggerSelectionGeometry;
import io.github.kxyr0.echodebug.forge.client.ClientSelectionMirror;
import io.github.kxyr0.echodebug.forge.client.catalog.ClientAreaCatalog;
import io.github.kxyr0.echodebug.forge.config.EchoDebugConfig;
import io.github.kxyr0.echodebug.forge.wand.TriggerWandUseGuard;
import io.github.kxyr0.echotrigger.api.authoring.catalog.AreaGeometrySnapshot;
import io.github.kxyr0.echotrigger.api.authoring.catalog.TriggerAreaDefinitionView;
import io.github.kxyr0.echotrigger.api.geometry.AreaShapeKind;
import io.github.kxyr0.echotrigger.api.geometry.Vector3;
import io.github.kxyr0.echotrigger.api.id.TriggerId;
import io.github.kxyr0.echotrigger.api.id.TriggerRevision;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class TriggerWorldOverlayRenderer {
    private static final OverlayColor CATALOG_COLOR = new OverlayColor(62, 255, 142, 72);
    private static final OverlayColor PREVIEW_COLOR = new OverlayColor(70, 210, 255, 88);
    private static final OverlayColor POINT_COLOR = new OverlayColor(255, 184, 55, 120);
    private final ClientSelectionMirror selection;
    private final ClientAreaCatalog catalog;
    private final TriggerAreaRenderPlanner planner = new TriggerAreaRenderPlanner();
    private final VanillaTriggerOverlayDrawer drawer = new VanillaTriggerOverlayDrawer();

    public TriggerWorldOverlayRenderer(ClientSelectionMirror selection, ClientAreaCatalog catalog) {
        this.selection = selection;
        this.catalog = catalog;
    }

    @SubscribeEvent
    public void onRenderLevel(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!active(minecraft) || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Vector3 camera = camera(event);
        List<ColoredPlan> plans = plans(minecraft, camera);
        if (plans.isEmpty()) {
            return;
        }
        drawer.draw(event.getPoseStack(), camera, plans);
    }

    private List<ColoredPlan> plans(Minecraft minecraft, Vector3 camera) {
        String dimension = minecraft.level.dimension().location().toString();
        List<ColoredPlan> result = new ArrayList<>();
        catalog.areas().stream()
                .flatMap(area -> planner.plan(area, dimension, camera, EchoDebugConfig.renderDistance()).stream())
                .map(plan -> new ColoredPlan(plan, CATALOG_COLOR))
                .forEach(result::add);
        preview(dimension, camera).map(plan -> new ColoredPlan(plan, PREVIEW_COLOR)).ifPresent(result::add);
        pointPreviews(dimension, camera).stream()
                .map(plan -> new ColoredPlan(plan, POINT_COLOR)).forEach(result::add);
        return List.copyOf(result);
    }

    private Optional<TriggerAreaRenderPlan> preview(String dimension, Vector3 camera) {
        return selection.snapshot()
                .filter(snapshot -> snapshot.geometry().isPresent())
                .flatMap(snapshot -> planner.plan(previewArea(snapshot.geometry().orElseThrow(), dimension),
                        dimension, camera, EchoDebugConfig.renderDistance()));
    }

    private List<TriggerAreaRenderPlan> pointPreviews(String dimension, Vector3 camera) {
        if (selection.snapshot().isEmpty()) {
            return List.of();
        }
        List<TriggerBlockPoint> points = selection.snapshot().orElseThrow().points();
        List<TriggerAreaRenderPlan> result = new ArrayList<>();
        for (int index = 0; index < points.size(); index++) {
            TriggerBlockPoint point = points.get(index);
            planner.plan(pointArea(point, index), dimension, camera, EchoDebugConfig.renderDistance())
                    .ifPresent(result::add);
        }
        return List.copyOf(result);
    }

    private TriggerAreaDefinitionView previewArea(TriggerSelectionGeometry geometry, String dimension) {
        AreaGeometrySnapshot area = new AreaGeometrySnapshot(geometry.shapeKind(), geometry.min(), geometry.max(),
                geometry.center(), geometry.radius(), geometry.polygonPoints());
        return area("jdh:debug_preview", "Preview", dimension, area);
    }

    private TriggerAreaDefinitionView pointArea(TriggerBlockPoint point, int index) {
        Vector3 min = point.lowerCorner();
        Vector3 max = point.upperExclusiveCorner();
        AreaGeometrySnapshot area = new AreaGeometrySnapshot(AreaShapeKind.BOX, min, max,
                min.add(max).divide(2.0D), OptionalDouble.empty(), List.of());
        return area("jdh:debug_point_" + index, "Point " + (index + 1), point.dimension(), area);
    }

    private static TriggerAreaDefinitionView area(
            String id,
            String name,
            String dimension,
            AreaGeometrySnapshot geometry
    ) {
        return new TriggerAreaDefinitionView(new TriggerId(id), name, dimension,
                new TriggerRevision(0), geometry, Optional.empty(), List.of());
    }

    private static boolean active(Minecraft minecraft) {
        return minecraft.level != null && minecraft.player != null && TriggerWandUseGuard.held(minecraft.player);
    }

    private static Vector3 camera(RenderLevelStageEvent event) {
        var position = event.getCamera().getPosition();
        return new Vector3(position.x, position.y, position.z);
    }

    record ColoredPlan(TriggerAreaRenderPlan plan, OverlayColor color) {
    }

    record OverlayColor(int red, int green, int blue, int fillAlpha) {
    }
}
