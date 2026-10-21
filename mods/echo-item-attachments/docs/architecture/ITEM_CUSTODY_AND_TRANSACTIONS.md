# Item custody and transactions

`AttachmentLedger` keeps one `CustodyRecord` per real payload. Its state is one of
`WORLD_ITEM`, `ATTACHED_TO_SUBJECT`, `RELEASING`, `WORLD_RELEASED`, or `RECOVERING`.
The record contains a compressed NBT encoding produced from `ItemStack.save`, preserving item
ID, count, components/NBT, display name, enchantments, damage, and serialized capability data.

`TakeFlow` resolves exactly one living `ItemEntity` in an AABB no larger than 0.75 blocks.
It persists `WORLD_ITEM` before the marker and then a `RECOVERING` split intent before
shrinking the source. With animation, the source is untouched until the exact attach marker.
Recovery compares the durable pre-split count with the live source count to choose rollback,
complete-hand-off, or quarantine without guessing. The journal writes a temporary compressed NBT file, forces the
channel, then atomically replaces `data/echo_item_attachments.dat`.

`HandLeaseBook` and the ledger independently prevent two transitions or two payloads from
owning the same subject/hand. Vanilla physical-hand occupancy is also rejected. Command IDs
are idempotent through `CommandLedger` plus argument fingerprints; cancellation restores a reserved world transition
without cloning the payload.
