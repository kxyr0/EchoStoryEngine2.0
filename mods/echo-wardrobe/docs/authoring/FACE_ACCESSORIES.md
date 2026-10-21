# Face accessories in W2

Glasses are modeled as a `FACE` wearable plus optional item model geometry. Portrait/runtime face skin comes from the exact full outfit PNG, not from a separate runtime face overlay.

```es
Id("jdh:origin_john_glasses");
Profile("jdh:john");
Item("jdh:origin_john_glasses");
Slot(FACE);
EquipmentSlot(HEAD);
Model("jdh:item/origin_john_glasses");
```

Consumers such as Dialogue portraits read the neutral Content Player resolved appearance boundary. They do not import Wardrobe implementation classes.
