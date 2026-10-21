package io.github.kxyr0.echoiteminspection.client.screen;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class InspectionTextScrollTest {
    @Test void everyLongDescriptionLineCanBeReachedWithPagesOrTheScrollbar() {
        var scroll = new InspectionTextScroll();
        scroll.resize(200, 10);
        scroll.page(1);
        assertEquals(9, scroll.position());
        scroll.move(Integer.MAX_VALUE);
        assertEquals(190, scroll.position());
        scroll.seek(0);
        assertEquals(0, scroll.position());
        scroll.seek(1);
        assertEquals(190, scroll.position());
        assertEquals(0.05, scroll.visibleFraction());
    }

    @Test void resizeAndInvalidInputNeverLeaveTheVisibleDocumentRange() {
        var scroll = new InspectionTextScroll();
        scroll.resize(20, 4);
        scroll.seek(1);
        scroll.resize(20, 15);
        assertEquals(5, scroll.position());
        scroll.seek(Double.NaN);
        assertEquals(5, scroll.position());
        scroll.move(Integer.MIN_VALUE);
        assertEquals(0, scroll.position());
        scroll.resize(2, 5);
        assertFalse(scroll.canScroll());
        assertEquals(1, scroll.visibleFraction());
    }
}
