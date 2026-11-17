package com.markvoronin.immichswipe.core.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomHeadersTest {

    @Test
    fun `ordinary header names are valid`() {
        assertTrue(CustomHeaders.isValidName("CF-Access-Client-Id"))
        assertTrue(CustomHeaders.isValidName("X_Custom.Header~1"))
    }

    @Test
    fun `names with spaces, colons or non-ASCII characters are invalid`() {
        assertFalse(CustomHeaders.isValidName(""))
        assertFalse(CustomHeaders.isValidName("bad name"))
        assertFalse(CustomHeaders.isValidName("Name:"))
        assertFalse(CustomHeaders.isValidName("Nomé"))
    }

    @Test
    fun `names the app or the HTTP stack sets are reserved whatever their case`() {
        assertFalse(CustomHeaders.isValidName("x-api-key"))
        assertFalse(CustomHeaders.isValidName("X-API-KEY"))
        assertFalse(CustomHeaders.isValidName(ImmichAuthInterceptor.ACCOUNT_HEADER))
        assertFalse(CustomHeaders.isValidName("Host"))
        assertFalse(CustomHeaders.isValidName("Content-Length"))
    }

    @Test
    fun `values need visible characters only`() {
        assertTrue(CustomHeaders.isValidValue("abc 123-_=.,;"))
        assertFalse(CustomHeaders.isValidValue(""))
        assertFalse(CustomHeaders.isValidValue("line\nbreak"))
        assertFalse(CustomHeaders.isValidValue("carriage\rreturn"))
        assertFalse(CustomHeaders.isValidValue("café"))
    }

    @Test
    fun `entries are trimmed and blank rows skipped`() {
        val headers = CustomHeaders.fromEntries(
            listOf(" A " to " 1 ", "" to "", "  " to " ", "B" to "2")
        )
        assertEquals(mapOf("A" to "1", "B" to "2"), headers)
    }

    @Test
    fun `no entries is an empty set of headers, not an error`() {
        assertEquals(emptyMap<String, String>(), CustomHeaders.fromEntries(emptyList()))
    }

    @Test
    fun `a half filled row is an error`() {
        assertNull(CustomHeaders.fromEntries(listOf("A" to "")))
        assertNull(CustomHeaders.fromEntries(listOf("" to "value")))
    }

    @Test
    fun `an invalid, reserved or repeated name is an error`() {
        assertNull(CustomHeaders.fromEntries(listOf("bad name" to "1")))
        assertNull(CustomHeaders.fromEntries(listOf("x-api-key" to "1")))
        assertNull(CustomHeaders.fromEntries(listOf("A" to "1", "a" to "2")))
    }

    @Test
    fun `headers survive being stored and read back`() {
        val headers = linkedMapOf("CF-Access-Client-Id" to "abc", "X-Token" to "a: b")

        val stored = CustomHeaders.encode(headers)

        assertEquals("CF-Access-Client-Id: abc\nX-Token: a: b", stored)
        assertEquals(headers, CustomHeaders.parse(stored))
    }

    @Test
    fun `no headers is stored as null`() {
        assertNull(CustomHeaders.encode(emptyMap()))
        assertEquals(emptyMap<String, String>(), CustomHeaders.parse(null))
        assertEquals(emptyMap<String, String>(), CustomHeaders.parse(""))
    }

    @Test
    fun `reading a stored value drops lines that are not valid headers`() {
        val stored = "Good: yes\nno colon here\nx-api-key: stolen\nbad name: 1\nEmpty:\n"

        assertEquals(mapOf("Good" to "yes"), CustomHeaders.parse(stored))
    }
}
