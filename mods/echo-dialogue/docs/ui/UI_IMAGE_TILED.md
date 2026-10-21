# UI Image TILED

`TILED` repeats source regions without stretching a tile.

If borders are zero, the whole source region repeats on X and Y. If borders are non-zero:

- corners are drawn once;
- top and bottom edges repeat on X;
- left and right edges repeat on Y;
- center repeats on X and Y.

Default behavior draws clipped partial tiles on the far edges. This fills the whole destination rect and
keeps dialogue panels from getting transparent holes.

Optional full-only behavior is controlled by `tileFullOnly: true`. In that mode, incomplete edge
fragments are skipped. Use it only for decorative patterns where clipped fragments create visible dots.

Example with default clipped edges: source tile `29x28`, destination `35x33`.

- first tile: destination `29x28`, source `29x28`;
- right partial tile: destination `6x28`, source `6x28`;
- bottom partial tile: destination `29x5`, source `29x5`;
- bottom-right partial tile: destination `6x5`, source `6x5`.

Safety guard: if one image would require more than `4096` tile draw calls, the renderer raises
`UI_IMAGE_TILE_ITERATION_LIMIT`.
