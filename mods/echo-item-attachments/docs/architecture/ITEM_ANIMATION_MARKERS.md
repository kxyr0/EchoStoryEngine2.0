# Item animation markers

The generic IDs used by item choreography are `echo_item_attachments:attach_left_hand`,
`echo_item_attachments:attach_right_hand`, `echo_item_attachments:release`, and
`echo_item_attachments:detach`. `AttachmentAnimationStarter` verifies that the requested animation exists and has
exactly one requested marker before starting playback.

Every transition records `MarkerBinding`: command, attachment subject, animation,
playback ID, playback revision, phase, and marker ID. `AttachmentMarkerListener` forwards the
authoritative `AnimationMarkerEvent`; `MarkerGate` rejects stale, duplicate, unbound, or
mismatched occurrences. A take leaves the `ItemEntity` in the world until its attach marker.
A move leaves the item on the old hand, and a drop/throw leaves it attached, until their exact
marker is accepted.

Echo Animation 0.8 adds `AnimationService.sampleAttachmentSocket`. Its server sampler is
pinned to the exact transition playback and uses the normalized clip sampler, rig hierarchy, and socket matrix.
`AnimationSocketAnchorProvider` converts that model-space sample to a world-space hand anchor
If playback becomes terminal/interrupted before the required marker, the adapter cancels the
transition and leaves the real item in its pre-marker custody location.
