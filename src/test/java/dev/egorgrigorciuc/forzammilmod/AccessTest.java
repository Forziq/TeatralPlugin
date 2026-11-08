package dev.egorgrigorciuc.forzammilmod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessTest {

    @Test
    void ownerNameIsExact() {
        assertTrue(Access.isOwnerName("Forzammil"));
        assertFalse(Access.isOwnerName("forzammil"));
        assertFalse(Access.isOwnerName(null));
    }
}
