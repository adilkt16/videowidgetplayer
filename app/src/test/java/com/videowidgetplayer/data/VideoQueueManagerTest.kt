package com.videowidgetplayer.data

import android.content.Context
import com.videowidgetplayer.utils.PreferenceUtils
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class VideoQueueManagerTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    private lateinit var queueManager: VideoQueueManager
    private val testWidgetId = 123
    private val testVideoUris = listOf(
        "content://video1.mp4",
        "content://video2.mp4", 
        "content://video3.mp4",
        "content://video4.mp4"
    )
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        queueManager = VideoQueueManager.getInstance()
        
        // Clear any existing state
        queueManager.clearQueue(testWidgetId)
    }
    
    @Test
    fun testQueueInitialization() {
        // Initialize queue
        queueManager.initializeQueue(
            context = mockContext,
            widgetId = testWidgetId,
            videoUris = testVideoUris,
            startIndex = 0,
            shuffleEnabled = false,
            loopMode = VideoQueueManager.LoopMode.NONE
        )
        
        // Verify queue is created
        assertTrue("Queue should exist", queueManager.hasQueue(testWidgetId))
        
        val queue = queueManager.getQueue(testWidgetId)
        assertNotNull("Queue should not be null", queue)
        assertEquals("Queue size should match", testVideoUris.size, queue!!.videos.size)
        assertEquals("Current index should be 0", 0, queue.currentIndex)
        assertFalse("Shuffle should be disabled", queue.isShuffleEnabled)
        assertEquals("Loop mode should be NONE", VideoQueueManager.LoopMode.NONE, queue.loopMode)
    }
    
    @Test
    fun testNextVideoNavigation() {
        // Initialize queue
        queueManager.initializeQueue(
            context = mockContext,
            widgetId = testWidgetId,
            videoUris = testVideoUris,
            startIndex = 0,
            shuffleEnabled = false,
            loopMode = VideoQueueManager.LoopMode.NONE
        )
        
        // Test navigation to next video
        val nextVideo = queueManager.nextVideo(mockContext, testWidgetId)
        assertEquals("Next video should be second video", testVideoUris[1], nextVideo)
        
        val queue = queueManager.getQueue(testWidgetId)
        assertEquals("Current index should be 1", 1, queue!!.currentIndex)
    }
    
    @Test
    fun testPreviousVideoNavigation() {
        // Initialize queue starting at index 2
        queueManager.initializeQueue(
            context = mockContext,
            widgetId = testWidgetId,
            videoUris = testVideoUris,
            startIndex = 2,
            shuffleEnabled = false,
            loopMode = VideoQueueManager.LoopMode.NONE
        )
        
        // Test navigation to previous video
        val previousVideo = queueManager.previousVideo(mockContext, testWidgetId)
        assertEquals("Previous video should be second video", testVideoUris[1], previousVideo)
        
        val queue = queueManager.getQueue(testWidgetId)
        assertEquals("Current index should be 1", 1, queue!!.currentIndex)
    }
    
    @Test
    fun testLoopModeAll() {
        // Initialize queue with loop all
        queueManager.initializeQueue(
            context = mockContext,
            widgetId = testWidgetId,
            videoUris = testVideoUris,
            startIndex = testVideoUris.size - 1, // Last video
            shuffleEnabled = false,
            loopMode = VideoQueueManager.LoopMode.ALL
        )
        
        // Navigate to next should loop to first video
        val nextVideo = queueManager.nextVideo(mockContext, testWidgetId)
        assertEquals("Should loop to first video", testVideoUris[0], nextVideo)
        
        val queue = queueManager.getQueue(testWidgetId)
        assertEquals("Current index should be 0", 0, queue!!.currentIndex)
    }
    
    @Test
    fun testLoopModeSingle() {
        // Initialize queue with single loop at index 1
        queueManager.initializeQueue(
            context = mockContext,
            widgetId = testWidgetId,
            videoUris = testVideoUris,
            startIndex = 1,
            shuffleEnabled = false,
            loopMode = VideoQueueManager.LoopMode.SINGLE
        )
        
        // Navigate to next should stay at same video
        val nextVideo = queueManager.nextVideo(mockContext, testWidgetId)
        assertEquals("Should stay at same video", testVideoUris[1], nextVideo)
        
        val queue = queueManager.getQueue(testWidgetId)
        assertEquals("Current index should remain 1", 1, queue!!.currentIndex)
    }
    
    @Test
    fun testShuffleToggle() {
        // Initialize queue
        queueManager.initializeQueue(
            context = mockContext,
            widgetId = testWidgetId,
            videoUris = testVideoUris,
            startIndex = 0,
            shuffleEnabled = false,
            loopMode = VideoQueueManager.LoopMode.NONE
        )
        
        // Enable shuffle
        queueManager.toggleShuffle(mockContext, testWidgetId)
        assertTrue("Shuffle should be enabled", queueManager.isShuffleEnabled(testWidgetId))
        
        // Disable shuffle
        queueManager.toggleShuffle(mockContext, testWidgetId)
        assertFalse("Shuffle should be disabled", queueManager.isShuffleEnabled(testWidgetId))
    }
    
    @Test
    fun testNavigationBounds() {
        // Initialize queue at first position
        queueManager.initializeQueue(
            context = mockContext,
            widgetId = testWidgetId,
            videoUris = testVideoUris,
            startIndex = 0,
            shuffleEnabled = false,
            loopMode = VideoQueueManager.LoopMode.NONE
        )
        
        // Should not navigate to previous when at first position
        assertFalse("Should not have previous", queueManager.hasPrevious(testWidgetId))
        val previousVideo = queueManager.previousVideo(mockContext, testWidgetId)
        assertNull("Previous video should be null", previousVideo)
        
        // Navigate to last position
        queueManager.jumpToVideo(mockContext, testWidgetId, testVideoUris.size - 1)
        
        // Should not navigate to next when at last position
        assertFalse("Should not have next", queueManager.hasNext(testWidgetId))
        val nextVideo = queueManager.nextVideo(mockContext, testWidgetId)
        assertNull("Next video should be null", nextVideo)
    }
}
