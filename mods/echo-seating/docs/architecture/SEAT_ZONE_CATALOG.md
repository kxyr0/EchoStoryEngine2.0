# SeatZone Catalog

`SeatZoneCatalog` stores immutable zone snapshots and owns `SeatZoneSpatialIndex`.

Zones are indexed by dimension and chunk cell. Query cost depends on the changed riding anchor, not on all blocks inside all zones.

Catalog validation rejects overlapping zones in the same dimension for SE1.
