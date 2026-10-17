package com.ailauncher.app.domain.search

import java.text.Normalizer
import kotlin.math.min

/**
 * v10: how well an app label matches what the user typed — the scoring behind
 * both the Apps page search and Global Search.
 *
 * Beyond plain substring matching it handles:
 * - **Hebrew ↔ Latin**: "וואטסאפ" finds WhatsApp and "walla" finds וואלה. Both
 *   sides are reduced to a consonant skeleton ([phoneticKey]) that Hebrew
 *   spelling and English spelling of the same brand name share.
 * - **Typos**: one edit tolerated for 4–6 letter queries, two from 7.
 * - **Initials**: "gm" → Google Maps.
 * - Accents, niqqud and Hebrew final letters are ignored (ך = כ, etc.).
 *
 * Returns 0 for no match; higher is better. Pure Kotlin — unit-tested on the JVM.
 */
object SearchMatcher {
    fun score(
        query: String,
        label: String,
        packageName: String = "",
    ): Int {
        val q = normalize(query)
        if (q.isEmpty()) return 0
        val l = normalize(label)
        if (l.isEmpty()) return 0
        val words = l.split(' ').filter { it.isNotEmpty() }

        if (l == q) return 100
        if (l.startsWith(q)) return 95
        if (words.any { it.startsWith(q) }) return 90
        if (l.contains(q)) return 75

        val qCompact = q.replace(" ", "")
        if (qCompact.length >= 2 && words.size >= 2 &&
            words.joinToString("") { it.take(1) }.startsWith(qCompact)
        ) {
            return 65
        }

        val qKey = phoneticKey(q)
        if (qKey.length >= 2) {
            val lKey = phoneticKey(l)
            if (lKey.startsWith(qKey)) return 60
            if (words.any { phoneticKey(it).startsWith(qKey) }) return 58
            if (qKey.length >= 3 && lKey.contains(qKey)) return 50
        }

        if (qCompact.length >= 4) {
            val allowed = if (qCompact.length >= 7) 2 else 1
            val candidates = words + l.replace(" ", "")
            if (candidates.any { w -> w.length >= qCompact.length - allowed && prefixDistance(qCompact, w) <= allowed }) return 40
        }

        if (packageName.isNotEmpty() && q.length >= 3 && packageName.lowercase().contains(qCompact)) return 30
        return 0
    }

    /** Lowercase, strip accents/niqqud, fold Hebrew final letters, collapse punctuation to spaces. */
    fun normalize(s: String): String {
        val decomposed = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
        val sb = StringBuilder(decomposed.length)
        for (c in decomposed) {
            when {
                Character.getType(c) == Character.NON_SPACING_MARK.toInt() -> {}

                // accents + niqqud
                c in HEBREW_FINALS -> {
                    sb.append(HEBREW_FINALS.getValue(c))
                }

                c.isLetterOrDigit() -> {
                    sb.append(c)
                }

                else -> {
                    sb.append(' ')
                }
            }
        }
        return sb.toString().trim().replace(Regex(" +"), " ")
    }

    /**
     * Consonant skeleton shared by Hebrew and Latin spellings of a name.
     * Vowels (and Hebrew letters used as vowels — א, ע, י, a single medial ו,
     * a final ה) are dropped; letters that sound alike map to one class
     * (b/v/w/ב/וו → B, p/f/ph/פ → P, k/q/c/ck/כ/ק → K, t/ט/ת → T, s/ס/ש/sh → S,
     * j/g/ג → G, h/ח/ה → H; צ and "tz" → T+S, matching how "ts" is spelled
     * in both scripts, e.g. WhatsApp/וואטסאפ); runs of the same class collapse.
     */
    fun phoneticKey(s: String): String {
        val out = StringBuilder()

        fun add(cls: Char) {
            if (out.isEmpty() || out.last() != cls) out.append(cls)
        }
        val n = normalize(s)
        var i = 0
        while (i < n.length) {
            val c = n[i]
            val next = n.getOrNull(i + 1)
            when (c) {
                // ── Latin ──
                'a', 'e', 'i', 'o', 'u', 'y' -> {}

                'w' -> {
                    add('B')
                    if (next == 'h') i++
                }

                'b', 'v' -> {
                    add('B')
                }

                'p' -> {
                    add('P')
                    if (next == 'h') i++
                }

                'f' -> {
                    add('P')
                }

                'c' -> {
                    when (next) {
                        'h' -> {
                            add('K')
                            i++
                        }

                        'e', 'i', 'y' -> {
                            add('S')
                        }

                        'k' -> {
                            add('K')
                            i++
                        }

                        else -> {
                            add('K')
                        }
                    }
                }

                'k', 'q' -> {
                    add('K')
                }

                'x' -> {
                    add('K')
                    add('S')
                }

                'g', 'j' -> {
                    add('G')
                }

                'd' -> {
                    add('D')
                }

                't' -> {
                    when (next) {
                        'z' -> {
                            add('T')
                            add('S')
                            i++
                        }

                        // "tz" is how צ is usually romanized
                        'h' -> {
                            add('T')
                            i++
                        }

                        else -> {
                            add('T')
                        }
                    }
                }

                's' -> {
                    add('S')
                    if (next == 'h') i++
                }

                'z' -> {
                    add('Z')
                }

                'l' -> {
                    add('L')
                }

                'm' -> {
                    add('M')
                }

                'n' -> {
                    add('N')
                }

                'r' -> {
                    add('R')
                }

                'h' -> {
                    add('H')
                }

                // ── Hebrew ──
                'א', 'ע', 'י' -> {}

                'ב' -> {
                    add('B')
                }

                'ו' -> {
                    // וו is a consonant (w/v); a single ו inside a word is a vowel (o/u);
                    // a word-initial single ו is "v".
                    val prev = n.getOrNull(i - 1)
                    when {
                        next == 'ו' -> {
                            add('B')
                            i++
                        }

                        prev == null || prev == ' ' -> {
                            add('B')
                        }

                        else -> {}
                    }
                }

                'ג' -> {
                    add('G')
                }

                'ד' -> {
                    add('D')
                }

                'ה' -> {
                    if (next != null && next != ' ') add('H')
                }

                // final ה is a vowel
                'ז' -> {
                    add('Z')
                }

                'ח' -> {
                    add('H')
                }

                'ט', 'ת' -> {
                    add('T')
                }

                'כ', 'ק' -> {
                    add('K')
                }

                'ל' -> {
                    add('L')
                }

                'מ' -> {
                    add('M')
                }

                'נ' -> {
                    add('N')
                }

                'ס', 'ש' -> {
                    add('S')
                }

                'פ' -> {
                    add('P')
                }

                'צ' -> {
                    add('T')
                    add('S')
                }

                'ר' -> {
                    add('R')
                }

                ' ' -> {}

                else -> {
                    if (c.isDigit()) add(c)
                }
            }
            i++
        }
        return out.toString()
    }

    /** Edit distance between [q] and the best-matching prefix of [word] (typo tolerance while typing). */
    internal fun prefixDistance(
        q: String,
        word: String,
    ): Int {
        val maxLen = min(word.length, q.length + 2)
        var best = Int.MAX_VALUE
        for (len in maxOf(1, q.length - 2)..maxLen) {
            best = min(best, damerauLevenshtein(q, word.substring(0, len)))
        }
        return best
    }

    internal fun damerauLevenshtein(
        a: String,
        b: String,
    ): Int {
        val d = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) d[i][0] = i
        for (j in 0..b.length) d[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                d[i][j] = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    d[i][j] = min(d[i][j], d[i - 2][j - 2] + 1)
                }
            }
        }
        return d[a.length][b.length]
    }

    private val HEBREW_FINALS = mapOf('ך' to 'כ', 'ם' to 'מ', 'ן' to 'נ', 'ף' to 'פ', 'ץ' to 'צ')
}
