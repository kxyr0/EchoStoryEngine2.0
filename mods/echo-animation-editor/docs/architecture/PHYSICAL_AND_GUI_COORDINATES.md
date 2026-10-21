# Physical And GUI Coordinates

Rendering panels are authored in physical framebuffer pixels. Before Minecraft GUI rendering, each rectangle is mapped
to GUI coordinates by `AnimatorPanelRect#scaleToGui`.

Unit tests prove that GUI scale 1-4 keeps identical physical rectangles for 1920x1080.
