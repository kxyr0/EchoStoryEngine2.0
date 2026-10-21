package io.github.kxyr0.echotimehud.geometry;

public record HudLayoutResult(double uniformScale, PhysicalRect framebuffer, PhysicalRect referenceCanvas,
                              PhysicalRect safeRect, PhysicalRect rawTextRect, PhysicalRect finalTextRect) {
    public double guiX(double framebufferPerGuiX) { return finalTextRect.x() / framebufferPerGuiX; }
    public double guiY(double framebufferPerGuiY) { return finalTextRect.y() / framebufferPerGuiY; }
    public double guiTextScale(double framebufferPerGuiX) { return uniformScale / framebufferPerGuiX; }
}

