package com.markvoronin.immichswipe.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlPolicyTest {

    @Test
    fun `https is never flagged`() {
        assertFalse(UrlPolicy.isInsecureRemote("https://photos.example.com"))
        assertFalse(UrlPolicy.isInsecureRemote("https://203.0.113.10:2283"))
    }

    @Test
    fun `http on private networks is allowed silently`() {
        listOf(
            "http://10.0.0.10:2283",
            "http://192.168.1.5:2283",
            "http://172.16.0.2",
            "http://172.31.255.1",
            "http://127.0.0.1:2283",
            "http://100.64.0.7:2283",
            "http://169.254.1.1",
            "http://localhost:2283",
            "http://nas:2283",
            "http://immich.local",
            "http://photos.home.arpa",
            "http://box.tailnet-name.ts.net:2283",
            "http://[::1]:2283",
            "http://[fd12:3456::1]:2283",
            "http://[fe80::1]:2283"
        ).forEach { assertFalse("$it should be allowed", UrlPolicy.isInsecureRemote(it)) }
    }

    @Test
    fun `http to public hosts is flagged`() {
        listOf(
            "http://photos.example.com",
            "http://203.0.113.10:2283",
            "http://172.32.0.1",   // just outside 172.16/12
            "http://172.15.0.1",
            "http://100.128.0.1",  // just outside 100.64/10
            "http://[2001:db8::1]:2283"
        ).forEach { assertTrue("$it should be flagged", UrlPolicy.isInsecureRemote(it)) }
    }

    @Test
    fun `unparseable input is not flagged`() {
        assertFalse(UrlPolicy.isInsecureRemote(""))
        assertFalse(UrlPolicy.isInsecureRemote("not a url"))
    }
}
