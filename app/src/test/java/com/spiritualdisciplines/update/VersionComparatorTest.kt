package com.spiritualdisciplines.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {
    @Test
    fun detectsNewerVersions() {
        assertTrue(VersionComparator.isNewer("1.1.0", "1.0.1"))
        assertTrue(VersionComparator.isNewer("v2.0", "1.9.9"))
        assertTrue(VersionComparator.isNewer("1.0.10", "1.0.9"))
    }

    @Test
    fun rejectsEqualAndOlderVersions() {
        assertFalse(VersionComparator.isNewer("1.0.1", "1.0.1"))
        assertFalse(VersionComparator.isNewer("1.0", "1.0.0"))
        assertFalse(VersionComparator.isNewer("1.0.0", "1.1.0"))
    }

    @Test
    fun ignoresTagPrefixAndPrereleaseSuffix() {
        assertTrue(VersionComparator.isNewer("v1.2.0-beta.1", "1.1.9"))
        assertFalse(VersionComparator.isNewer("v1.2.0", "1.2.0"))
    }
}
