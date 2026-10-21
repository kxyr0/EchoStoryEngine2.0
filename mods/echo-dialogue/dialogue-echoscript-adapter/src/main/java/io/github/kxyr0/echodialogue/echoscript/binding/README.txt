SRP split for NodeContentReader

Replace the old NodeContentReader.java with the included files in the same Java package:
io.github.kxyr0.echodialogue.echoscript.binding

Files:
- NodeContentReader.java: node assembly and routing only
- NodeCallReader.java: handler contract
- DialogueNodeCallReader.java: Line, Choice, Complete, Delay and transitions
- NpcNodeCallReader.java: all Npc.* commands, including Npc.WaitForArrival
- VisualNodeCallReader.java: face and blink commands
- ExternalNodeCallReader.java: cutscene, player, screen and effect commands

DSL usage:
Npc.Move("sasha", 46, -3, 94);
Npc.WaitForArrival("sasha");
Npc.EquipBackpack("sasha", Position(46, -3, 95));
