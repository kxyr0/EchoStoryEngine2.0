# Chair Integration Audit

Instance inspected:
`C:\Users\kxyr0\AppData\Roaming\PrismLauncher\instances\Forma Mysli\minecraft\mods`

Installed chair/furniture candidates:

- `mcw-furniture-3.4.1-mc1.20.1forge.jar`
  - mod ID: `mcwfurnitures`
  - chair block IDs include `mcwfurnitures:*_chair`, `*_modern_chair`,
    `*_stool_chair`, and `*_striped_chair`
  - bytecode quick check did not expose a public/static seat entity boundary
    comparable to `refurbished_furniture` in `ClassicChair`
  - adapter selected: none in PA0

- `refurbished_furniture-forge-1.20.1-1.0.20.jar`
  - mod ID: `refurbished_furniture`
  - chair block IDs include `refurbished_furniture:*_chair`
  - seat mechanism: `com.mrcrayfish.furniture.refurbished.entity.Seat`
    entity; the player becomes a passenger of that seat entity
  - relevant bytecode boundary:
    `Seat.availableAt(Level, BlockPos)` and
    `Seat.sit(Player, BlockPos, double, Direction)`
  - adapter selected:
    `echo_player_actor_refurbished_furniture_adapter`

The save `New World` was inspected at the authored PA0 chair coordinate
`(121, 64, 194)` in region `r.0.0.mca`; that exact block currently resolves to
`minecraft:air`. The opening content therefore keeps the provided coordinate,
but runtime `SitAt` will correctly fail with `PLAYER_ACTOR_SEAT_NOT_FOUND` until
the world contains a supported chair block at that position.

Adapter limits:

- Supports `refurbished_furniture:*_chair` only.
- Does not support `mcwfurnitures` chairs, sofas, stools, or generic furniture.
- Uses reflection to avoid a compile dependency on the chair mod implementation.
- Confirms success only when the player is a passenger of the Refurbished
  Furniture `Seat` entity at the authored block position.
