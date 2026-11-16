package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app_name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("سُكون - Nocturne", appName)
    }

    @Test
    fun `buildRealmeCompatibleMediaItem populates metadata and fallback cover bytes`() {
        val item = PlaybackService.buildRealmeCompatibleMediaItem(
            mediaId = "track_1",
            audioUri = Uri.parse("asset:///silent_keepalive.wav"),
            title = "سُكون",
            artist = "فنان",
            album = "ألبوم"
        )
        assertEquals("سُكون", item.mediaMetadata.title.toString())
        assertEquals("فنان", item.mediaMetadata.artist.toString())
        assertNotNull(item.mediaMetadata.artworkData)
        assertTrue((item.mediaMetadata.artworkData?.size ?: 0) > 0)
    }
}
