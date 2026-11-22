package com.ars.fx.shell;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DeleteKeyTest {
    @Test
    void onlyAFullSelectionOfSeveralCharactersIsDropped() {
        assertTrue(DeleteKey.selectsAll(5, 5));
        assertFalse(DeleteKey.selectsAll(5, 2));      // a partial selection: Delete removes it as usual
        assertFalse(DeleteKey.selectsAll(1, 1));      // one character: nothing to save
        assertFalse(DeleteKey.selectsAll(0, 0));
    }
}
