package app.gamenative.ui.screen.wow

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FitScreenSizeTest {

    @Test
    fun sixteenByNineWindow_keepsBaseSize() {
        assertEquals("1920x1080", fitScreenSize("1920x1080", 2400, 1350))
    }

    @Test
    fun foldInnerScreen_keepsPixelBudget() {
        assertEquals("1104x834", fitScreenSize("1280x720", 2448, 1848))
    }

    @Test
    fun portraitWindow_usesLandscapeAspect() {
        assertEquals("1104x834", fitScreenSize("1280x720", 1848, 2448))
    }

    @Test
    fun adreno740Base_onFoldInnerScreen() {
        assertEquals("1658x1252", fitScreenSize("1920x1080", 2448, 1848))
    }

    @Test
    fun widePhone_widensAndShortens() {
        assertEquals("2146x966", fitScreenSize("1920x1080", 2400, 1080))
    }

    @Test
    fun unknownWindow_keepsBaseSize() {
        assertEquals("1280x720", fitScreenSize("1280x720", 0, 0))
    }

    @Test
    fun fitsScreen_acceptsSameOrSmallerResolution() {
        assertTrue(fitsScreen("1104x834", "1104x834"))
        assertTrue(fitsScreen("800x600", "1104x834"))
    }

    @Test
    fun fitsScreen_rejectsLargerOrInvalidResolution() {
        assertFalse(fitsScreen("1920x1080", "1104x834"))
        assertFalse(fitsScreen("1206x764", "1104x834"))
        assertFalse(fitsScreen("auto", "1104x834"))
    }

    @Test
    fun resolutionOptions_followAspectUpToNative() {
        val expected = listOf("1148x720", "1276x800", "1436x900", "1724x1080", "1914x1200", "2298x1440", "2400x1504")
        assertEquals(expected, Resolution.options(2400, 1504))
        assertEquals(expected, Resolution.options(1504, 2400))
    }

    @Test
    fun resolutionResolve_usesPickedSizeForCurrentAspect() {
        assertEquals("1724x1080", Resolution.resolve("1724x1080", "1920x1080", 2400, 1504))
    }

    @Test
    fun resolutionResolve_fallsBackToAutoForAutoOrOtherAspect() {
        assertEquals("1104x834", Resolution.resolve(Resolution.AUTO, "1280x720", 2448, 1848))
        assertEquals("1104x834", Resolution.resolve("1724x1080", "1280x720", 2448, 1848))
    }
}
