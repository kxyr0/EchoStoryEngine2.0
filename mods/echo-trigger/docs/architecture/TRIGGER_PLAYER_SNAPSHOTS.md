# Player Snapshots

Snapshots are immutable server-thread reads. Worker code may compare snapshots but may not touch mutable Minecraft
objects.

Snapshot revisions:

- inventory revision
- hand revision
- equipment revision
- physical state revision
- effect revision
- progression revision
- world membership revision

Dirty revisions choose affected trigger instances. Unchanged players do not cause full catalog scans.
