package com.gymtrack.domain.identity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserFirstNameTest {

    @Test
    fun firstNameFromDisplayName_fullName_returnsFirstToken() {
        assertEquals("Danilo", firstNameFromDisplayName("Danilo Barros"))
    }

    @Test
    fun firstNameFromDisplayName_singleName_returnsSame() {
        assertEquals("Danilo", firstNameFromDisplayName("Danilo"))
    }

    @Test
    fun firstNameFromDisplayName_extraWhitespace_trimsAndReturnsFirst() {
        assertEquals("Danilo", firstNameFromDisplayName(" Danilo Barros "))
    }

    @Test
    fun firstNameFromDisplayName_null_returnsNull() {
        assertNull(firstNameFromDisplayName(null))
    }

    @Test
    fun firstNameFromDisplayName_blank_returnsNull() {
        assertNull(firstNameFromDisplayName(""))
        assertNull(firstNameFromDisplayName("   "))
    }
}
