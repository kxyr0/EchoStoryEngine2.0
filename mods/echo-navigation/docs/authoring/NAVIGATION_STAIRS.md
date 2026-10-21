# Navigation Stairs

The A* backend classifies vertical surface transitions as typed edges. Descending stairs and steps preserve forward
movement across the block boundary, and progress is measured along the edge rather than by raw Y distance.
