# Echo Item Attachments project map

The repository is independent and publishes neutral contracts from `item-attachments-contracts`.
`item-attachments-core` owns `AttachmentLedger`, `HandLeaseBook`, `MarkerGate`,
`CommandLedger`, `HandVelocityWindow`, and `RecoveryPlanner`; neither neutral module imports
Minecraft, Forge, Echo NPC, or Echo Animation.

`item-attachments-forge-api-1.20.1` exposes the event bridges used by optional adapters.
`item-attachments-forge-1.20.1` contains `ServerAttachmentHost`, its four narrow flows,
the atomic `AttachmentJournal`, network snapshots, release witnesses, commands, and recovery.
NPC, Player Actor, Animation, Story/EchoScript, and GameTest responsibilities live in their
named adapter modules. The production JAR embeds contracts, core, and Forge API, but never an
optional mod's classes.
