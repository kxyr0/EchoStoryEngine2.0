# NPC Interaction Project Map

Реальные классы и пути:

```text
EchoScript
-> npc-interaction-script-adapter-1.20.1/.../NpcInteractionScriptFacade.java
-> npc-interaction-forge-api-1.20.1/.../NpcInteractionService.java
-> npc-interaction-forge-1.20.1/.../runtime/ServerInteractionRuntime.java
-> npc-interaction-forge-1.20.1/.../target/ServerTargetInspector.java
-> npc-interaction-core/.../provider/NpcInteractionProviderSelector.java
-> npc-interaction-core/.../approach/NpcInteractionApproachResolver.java
-> echo-npc-interaction-navigation-adapter/.../NpcInteractionNavigationPort.java
-> npc-interaction-forge-1.20.1/.../runtime/NpcInteractionAlignment.java
-> npc-interaction-forge-1.20.1/.../provider/VanillaBlockUseProvider.java
-> npc-interaction-forge-1.20.1/.../proxy/ProxyUsePipeline.java
-> npc-interaction-forge-1.20.1/.../runtime/InteractionDiagnosticsLog.java
-> npc-interaction-contracts/.../result/NpcInteractionReceipt.java
-> Story/Dialogue wait listeners resume script
```

Navigation adapter зависит только от public APIs Echo NPC Interaction, Echo
Navigation и Echo NPC.
# NI2 Project Map

```text
Npc.Interact
-> npc-interaction-script-adapter-1.20.1/.../NpcInteractionScriptFacade.java
-> npc-interaction-forge-api-1.20.1/.../NpcInteractionService.java
-> npc-interaction-forge-1.20.1/.../InteractionStartFlow.java
-> npc-interaction-navigation-adapter/.../NpcInteractionNavigationPort.java
-> npc-interaction-forge-1.20.1/.../NpcInteractionAlignment.java
-> npc-interaction-forge-1.20.1/.../ProxyUsePipeline.java
-> npc-interaction-forge-1.20.1/.../InteractionEffectJournal.java
-> npc-interaction-forge-1.20.1/.../NpcInteractionConsequenceClassifier.java
-> npc-interaction-forge-1.20.1/.../NpcRideHandoff.java
-> npc-interaction-forge-1.20.1/.../EchoNpcSeatAnchorEntity.java
-> npc-interaction-forge-1.20.1/.../NpcSeatOccupancyRegistry.java
-> npc-interaction-contracts/.../NpcInteractionReceipt.java
-> script resume
```
