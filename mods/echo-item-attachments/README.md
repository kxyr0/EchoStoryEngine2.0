# Echo Item Attachments

Forge 1.20.1 infrastructure for moving one real `ItemStack` witness between a world item,
an NPC or player hand, another hand, and a released world item.

The server owns custody, persistence, marker validation and release physics. Clients only
render revisioned snapshots with Minecraft's `ItemRenderer`.

See [the project map](docs/architecture/ITEM_ATTACHMENTS_PROJECT_MAP.md) and
[the runtime DSL](docs/runtime/ITEM_ATTACHMENT_DSL.md).
