# Echo Dev Scenarios

```es
class ChemistryClass : DevScenarioResource
{
    Given()
    {
        Time.Set(Day(2), "09:55");
        Player.Place(UniversityHall);
        Npc.Ensure(Nolan);
        Npc.Ensure(Chris);
        Npc.Ensure(Halden);
    }

    Launch("jdh:chemistry_class");
}
```

Путь `dev/scenarios/chapter1/day2/chemistry.es` даёт launch ID `chapter1.day2.chemistry`; folder segments
автоматически формируют дерево launcher. Setup выполняется receipt-driven без sleeps, с
`SETUP_NO_SIDE_EFFECTS` и `SKIP_CROSSED`. `stop` восстанавливает только managed Echo state, а `restart` использует
тот же baseline, повторяет `Given` и запускает target заново.

Forge setup использует публичные Echo Time, Player Actor, NPC и Observation bridges. `Npc.Form(actor, form)`
передаётся в canonical `SetForm`; `Observation.Activate/Enable` доступны, когда соответствующий observation resource
есть в каталоге. Wardrobe остаётся query-only в текущем публичном API Echo Wardrobe, поэтому launcher не подменяет
его внутреннее состояние неофициальной записью.
