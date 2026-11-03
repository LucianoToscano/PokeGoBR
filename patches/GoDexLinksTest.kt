package br.com.pokegobr

import org.junit.Assert.*
import org.junit.Test

class GoDexLinksTest {
    @Test fun firstPartyPagesAreHttps() {
        for (url in listOf(GoDexLinks.home, GoDexLinks.shinyDex, GoDexLinks.maxDex)) {
            assertTrue(url.startsWith("https://godex.site/"))
        }
    }

    @Test fun exactPublicCollectionCanOpen() {
        val url = "https://godex.site/public-collection/30fceaa65b9db02479d8042198dd7f28"
        assertEquals(url, GoDexLinks.validPublicCollection(url))
        val ulid = "https://godex.site/public-collection/01KXCDWKYF994AVQEVCN4TGPYE"
        assertEquals(ulid, GoDexLinks.validPublicCollection(ulid))
    }

    @Test fun rejectUntrustedAndPrivateLinks() {
        for (url in listOf(
            "https://godex.site.evil.example/public-collection/30fceaa65b9db02479d8042198dd7f28",
            "http://godex.site/public-collection/30fceaa65b9db02479d8042198dd7f28",
            "https://godex.site/public-collection/30fceaa65b9db02479d8042198dd7f28?token=private",
            "https://godex.site/public-collection/../../private",
            "javascript:alert(1)",
            "https://godex.site/settings",
            "https://godex.site/login",
            ""
        )) {
            assertNull("Invalid collection URL must not open: $url", GoDexLinks.validPublicCollection(url))
        }
    }
}
