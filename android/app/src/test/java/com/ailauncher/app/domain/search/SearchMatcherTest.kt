package com.ailauncher.app.domain.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchMatcherTest {

    private fun matches(query: String, label: String) = SearchMatcher.score(query, label) > 0

    @Test
    fun `exact and prefix matches rank above substring matches`() {
        val exact = SearchMatcher.score("maps", "Maps")
        val prefix = SearchMatcher.score("ma", "Maps")
        val word = SearchMatcher.score("maps", "Google Maps")
        val inside = SearchMatcher.score("aps", "Maps")
        assertTrue(exact > prefix)
        assertTrue(prefix > word)
        assertTrue(word > inside)
        assertTrue(inside > 0)
    }

    @Test
    fun `Hebrew spellings find English app names`() {
        assertTrue(matches("וואטסאפ", "WhatsApp"))
        assertTrue(matches("גוגל", "Google"))
        assertTrue(matches("אינסטגרם", "Instagram"))
        assertTrue(matches("פייסבוק", "Facebook"))
        assertTrue(matches("יוטיוב", "YouTube"))
        assertTrue(matches("וויז", "Waze"))
        assertTrue(matches("ספוטיפיי", "Spotify"))
        assertTrue(matches("נטפליקס", "Netflix"))
        assertTrue(matches("טלגרם", "Telegram"))
        assertTrue(matches("כרום", "Chrome"))
    }

    @Test
    fun `English spellings find Hebrew app names`() {
        assertTrue(matches("walla", "וואלה"))
        assertTrue(matches("whatsapp", "וואטסאפ"))
    }

    @Test
    fun `phonetic keys of the same brand agree across scripts`() {
        assertEquals(SearchMatcher.phoneticKey("WhatsApp"), SearchMatcher.phoneticKey("וואטסאפ"))
        assertEquals(SearchMatcher.phoneticKey("Netflix"), SearchMatcher.phoneticKey("נטפליקס"))
        assertEquals(SearchMatcher.phoneticKey("pizza tz"), SearchMatcher.phoneticKey("pizza צ"))
    }

    @Test
    fun `small typos are tolerated`() {
        assertTrue(matches("whatsap", "WhatsApp"))
        assertTrue(matches("instagarm", "Instagram"))
        assertTrue(matches("spotfy", "Spotify"))
    }

    @Test
    fun `initials match multi-word labels`() {
        assertTrue(matches("gm", "Google Maps"))
    }

    @Test
    fun `unrelated queries do not match`() {
        assertEquals(0, SearchMatcher.score("xyz", "WhatsApp"))
        assertEquals(0, SearchMatcher.score("calendar", "Calculator"))
        assertEquals(0, SearchMatcher.score("", "WhatsApp"))
    }

    @Test
    fun `niqqud, accents and Hebrew final letters are ignored`() {
        assertEquals("שלומ", SearchMatcher.normalize("שָׁלוֹם"))
        assertTrue(matches("cafe", "Café"))
        assertTrue(matches("מחשבון", "מחשבון"))
    }

    @Test
    fun `package name is a last-resort match`() {
        assertTrue(SearchMatcher.score("tencent", "WeChat", "com.tencent.mm") > 0)
    }
}
