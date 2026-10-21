package io.github.kxyr0.echoquests.core.condition;

import io.github.kxyr0.echoquests.api.condition.QuestCondition;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class QuestConditionsTest {
    @Test
    void composesAllAnyAndNotWithoutGameplayDependencies() {
        QuestCondition yes = QuestCondition.leaf("test:yes", Map.of());
        QuestCondition no = QuestCondition.leaf("test:no", Map.of());
        QuestCondition any = new QuestCondition("any", Map.of(), List.of(no, yes));
        QuestCondition notNo = new QuestCondition("not", Map.of(), List.of(no));
        QuestCondition all = new QuestCondition("all", Map.of(), List.of(any, notNo));

        assertTrue(QuestConditions.test(all, condition -> condition.type().equals("test:yes")));
        assertFalse(QuestConditions.test(new QuestCondition("all", Map.of(), List.of(yes, no)),
                condition -> condition.type().equals("test:yes")));
    }
}
