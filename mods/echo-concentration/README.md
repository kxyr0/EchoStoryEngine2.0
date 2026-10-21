# Echo Concentration

Forge 1.20.1 client concentration lifecycle. The configurable `Z` key toggles the mode, the world is rendered through
Minecraft's desaturation post effect, and inventory opening is cancelled while concentration owns the `E` input.
Desaturation enters as an aspect-correct radial wave centered on the projected player position, falling back to the
camera center in first person, and reverses toward the player when concentration exits.

Other mods observe the typed lifecycle through `ConcentrationServiceBridge` and `ConcentrationLifecycleEvent`.

1.0.6: Ctrl/Shift/Alt/Super combinations no longer toggle concentration, preserving editor Undo/Redo shortcuts.
While an Oculus/Iris shaderpack is active, concentration keeps its lifecycle and sound treatment but skips the
vanilla desaturation PostChain to prevent framebuffer conflicts and a black world. Desaturation resumes when
shaderpacks are disabled. The optional API probe adds no required Oculus dependency.
