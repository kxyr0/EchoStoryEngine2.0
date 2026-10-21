# Timeline Architecture

`AnimatorTimelineGeometry` converts frames to pixels and pixels to frames with boundary clamping. The Forge screen draws
a playhead from this geometry; editing commands are deferred until public draft mutation APIs exist.
