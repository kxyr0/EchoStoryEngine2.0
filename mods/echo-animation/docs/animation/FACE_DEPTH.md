# Face layer depth precision

World face rendering preserves authored Position Z as an additional offset at scale 0.001.
Version 0.8.5 also reserves 0.001 world units per semantic plane: skin/sclera,
pupil, upper eyelid, lower eyelid, brow, glasses. This keeps overlapping textures
separate after float vertex conversion, including rigs authored with pupil Z=0.00001,
eyelid Z=0.0001 and brow Z=0.001. Upper and lower eyelids use separate planes;
left/right pieces of a layer share the same plane. Hat geometry remains in front
of all standard face layers. Large explicit Z values can still push a layer further forward.

High-level lower eyelid layer IDs start with `lower_eyelid`; custom lower eyelid
IDs should use that prefix to select the lower plane. Portrait rendering uses
its existing 2D ordering and is unchanged. No resource migration is needed.

Regression checks cover small authored depths, Ben-style depths, zero depths,
float conversion at local and camera coordinates, authored Z adjustments, and
the head/hat boundary. Forge tests verify the resolver called by the quad renderer.
