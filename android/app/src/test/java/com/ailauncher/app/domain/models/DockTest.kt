package com.ailauncher.app.domain.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class DockTest {
    private val present = setOf("a", "b", "c", "d", "e", "f")

    private fun withDock(vararg pkgs: String) = PagesSettings(pageLayouts = mapOf("home" to PageLayoutSettings(dockApps = pkgs.toList())))

    private fun PagesSettings.dock() = pageLayouts["home"]?.dockApps

    @Test fun `pinning appends and unpinning removes`() {
        val pinned = PagesSettings().toggleDockApp("a", present).toggleDockApp("b", present)
        assertEquals(listOf("a", "b"), pinned.dock())
        assertEquals(listOf("b"), pinned.toggleDockApp("a", present).dock())
    }

    @Test fun `a full dock is left unchanged`() {
        val full = withDock("a", "b", "c", "d", "e")
        assertSame(full, full.toggleDockApp("f", present))
        // Unpinning still works when full.
        assertEquals(listOf("a", "b", "d", "e"), full.toggleDockApp("c", present).dock())
    }

    @Test fun `apps that are gone don't hold a slot`() {
        // "x" was uninstalled or hidden: it doesn't count towards the five and is dropped on the next change.
        val dock = withDock("a", "x", "b", "c", "d")
        assertEquals(listOf("a", "b", "c", "d"), dock.dockPackages(present))
        assertEquals(listOf("a", "b", "c", "d", "e"), dock.toggleDockApp("e", present).dock())
    }

    @Test fun `other page layouts are kept`() {
        val s = PagesSettings(pageLayouts = mapOf("news" to PageLayoutSettings(pageId = "news")))
        val pinned = s.toggleDockApp("a", present)
        assertEquals(setOf("news", "home"), pinned.pageLayouts.keys)
    }
}
