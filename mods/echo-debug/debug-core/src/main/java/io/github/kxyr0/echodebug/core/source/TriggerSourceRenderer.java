package io.github.kxyr0.echodebug.core.source;

public final class TriggerSourceRenderer {
    private final TriggerShapeSourceRenderer shapes = new TriggerShapeSourceRenderer();

    public String render(TriggerSourceModel model) {
        String area = shapes.render(model.shape(), model.geometry());
        return """
                class %s : TriggerResource
                {
                    private TriggerArea Area = %s;

                    private void Init()
                    {
                        Id("%s");
                        Scope(%s);
                        Dimension("%s");
                        Bind(Area);
                        FireOnlyOnce();
                    }

                    private void OnTriggerEnter(PlayerRef player)
                    {
                    }

                    private void OnTriggerExit(TriggerEvent event)
                    {
                    }
                }
                """.formatted(model.id().className(), area, model.id().triggerId().value(),
                model.scope(), model.dimension());
    }
}
