package org.photocardlibre.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class AlbumStateTest {
    private val first = PhotoEntry("1", "/cache/1.jpg", "prima.jpg")
    private val second = PhotoEntry("2", "/cache/2.png", "seconda.png")
    private val third = PhotoEntry("3", "/cache/3.jpg", "terza.jpg")

    @Test
    fun addSelectMoveAndDeleteKeepDeterministicOrder() {
        var state = AlbumState().add(listOf(first, second, third))
        assertNull(state.selectedId)
        state = state.move("3", -1)
        assertEquals(listOf("1", "3", "2"), state.photos.map { it.id })
        assertNull(state.selectedId)
        state = state.select("3")
        val deleted = state.deleteSelected()
        assertEquals("3", deleted.removed?.id)
        assertEquals(listOf("1", "2"), deleted.state.photos.map { it.id })
        assertEquals("2", deleted.state.selectedId)
    }

    @Test
    fun captionIsUppercaseAndLimitedToFourWords() {
        val state = AlbumState().add(listOf(first)).select("1")
        val accepted = state.updateCaption("  al   mare oggi ")
        assertNull(accepted.error)
        assertEquals("AL MARE OGGI ", accepted.state.selectedPhoto?.caption)

        val rejected = accepted.state.updateCaption("uno due tre quattro cinque")
        assertEquals("La didascalia può contenere al massimo 4 parole.", rejected.error)
        assertSame(accepted.state, rejected.state)
    }

    @Test
    fun cropFollowsPhotoDuringReorderAndDisappearsWithDeletedPhoto() {
        val crop = CropRect(0.1, 0.2, 0.8, 0.6)
        var state = AlbumState().add(listOf(first, second)).select("1")
        state = state.updateSelectedCrop(crop).move("1", 1)

        assertEquals(listOf("2", "1"), state.photos.map { it.id })
        assertEquals(crop, state.photos.last().crop)

        val deleted = state.deleteSelected()
        assertEquals("1", deleted.removed?.id)
        assertEquals(crop, deleted.removed?.crop)
        assertNull(deleted.state.photos.first().crop)
    }
}
