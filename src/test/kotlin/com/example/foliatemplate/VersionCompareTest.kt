package com.example.foliatemplate

import com.example.foliatemplate.update.compareVersions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VersionCompareTest {

    @Test
    fun `orders versions numerically, not lexically`() {
        assertTrue(compareVersions("1.10.0", "1.9.9") > 0, "1.10.0 is newer than 1.9.9")
        assertTrue(compareVersions("2.0.0", "1.9.9") > 0)
        assertTrue(compareVersions("1.0.0", "1.0.1") < 0)
        assertEquals(0, compareVersions("1.2.3", "1.2.3"))
    }

    @Test
    fun `ignores leading v and differing lengths`() {
        assertEquals(0, compareVersions("v1.2.0", "1.2.0"))
        assertEquals(0, compareVersions("1.2", "1.2.0"))
        assertTrue(compareVersions("1.2.1", "1.2") > 0)
    }

    @Test
    fun `treats build and pre-release suffixes as non-numeric`() {
        assertTrue(compareVersions("1.2.0-SNAPSHOT", "1.1.0") > 0)
    }
}
