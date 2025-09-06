package com.videowidgetplayer.widgets

import android.content.Context
import com.videowidgetplayer.widgets.WidgetGestureManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class WidgetGestureManagerTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    private lateinit var gestureManager: WidgetGestureManager
    private val testWidgetId = 123
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        gestureManager = WidgetGestureManager.getInstance()
    }
    
    @Test
    fun testGestureSensitivityLevels() {
        val lowSensitivity = WidgetGestureManager.GestureSensitivity.fromLevel(
            WidgetGestureManager.SENSITIVITY_LOW
        )
        val mediumSensitivity = WidgetGestureManager.GestureSensitivity.fromLevel(
            WidgetGestureManager.SENSITIVITY_MEDIUM
        )
        val highSensitivity = WidgetGestureManager.GestureSensitivity.fromLevel(
            WidgetGestureManager.SENSITIVITY_HIGH
        )
        
        // Low sensitivity should require longer swipes
        assertTrue("Low sensitivity distance should be greater than medium", 
            lowSensitivity.minSwipeDistance > mediumSensitivity.minSwipeDistance)
        
        // High sensitivity should require shorter swipes
        assertTrue("High sensitivity distance should be less than medium",
            highSensitivity.minSwipeDistance < mediumSensitivity.minSwipeDistance)
        
        // Low sensitivity should allow more time
        assertTrue("Low sensitivity time should be greater than medium",
            lowSensitivity.maxSwipeTime > mediumSensitivity.maxSwipeTime)
    }
    
    @Test
    fun testGestureDirectionDetection() {
        val manager = WidgetGestureManager.getInstance()
        
        // Test left swipe (next video)
        val leftSwipeStart = WidgetGestureManager.TouchPoint(100f, 100f, 0L)
        val leftSwipeEnd = WidgetGestureManager.TouchPoint(50f, 105f, 200L)
        
        val leftGesture = manager.analyzeGesture(mockContext, testWidgetId, leftSwipeStart, leftSwipeEnd)
        assertEquals("Should detect left swipe", 
            WidgetGestureManager.SwipeDirection.LEFT, leftGesture?.direction)
        
        // Test right swipe (previous video)
        val rightSwipeStart = WidgetGestureManager.TouchPoint(50f, 100f, 0L)
        val rightSwipeEnd = WidgetGestureManager.TouchPoint(100f, 105f, 200L)
        
        val rightGesture = manager.analyzeGesture(mockContext, testWidgetId, rightSwipeStart, rightSwipeEnd)
        assertEquals("Should detect right swipe",
            WidgetGestureManager.SwipeDirection.RIGHT, rightGesture?.direction)
    }
    
    @Test
    fun testGestureConfidenceCalculation() {
        val manager = WidgetGestureManager.getInstance()
        
        // Perfect horizontal swipe should have high confidence
        val perfectStart = WidgetGestureManager.TouchPoint(0f, 100f, 0L)
        val perfectEnd = WidgetGestureManager.TouchPoint(200f, 100f, 150L)
        
        val perfectGesture = manager.analyzeGesture(mockContext, testWidgetId, perfectStart, perfectEnd)
        assertNotNull("Perfect gesture should be detected", perfectGesture)
        assertTrue("Perfect gesture should have high confidence",
            perfectGesture!!.confidence > 0.7f)
        
        // Diagonal swipe should have lower confidence
        val diagonalStart = WidgetGestureManager.TouchPoint(0f, 0f, 0L)
        val diagonalEnd = WidgetGestureManager.TouchPoint(150f, 100f, 200L)
        
        val diagonalGesture = manager.analyzeGesture(mockContext, testWidgetId, diagonalStart, diagonalEnd)
        if (diagonalGesture != null) {
            assertTrue("Diagonal gesture should have lower confidence",
                diagonalGesture.confidence < perfectGesture.confidence)
        }
    }
    
    @Test
    fun testGestureRejection() {
        val manager = WidgetGestureManager.getInstance()
        
        // Too short swipe should be rejected
        val shortStart = WidgetGestureManager.TouchPoint(100f, 100f, 0L)
        val shortEnd = WidgetGestureManager.TouchPoint(120f, 105f, 100L)
        
        val shortGesture = manager.analyzeGesture(mockContext, testWidgetId, shortStart, shortEnd)
        assertNull("Short swipe should be rejected", shortGesture)
        
        // Too slow swipe should be rejected
        val slowStart = WidgetGestureManager.TouchPoint(0f, 100f, 0L)
        val slowEnd = WidgetGestureManager.TouchPoint(200f, 105f, 2000L)
        
        val slowGesture = manager.analyzeGesture(mockContext, testWidgetId, slowStart, slowEnd)
        assertNull("Slow swipe should be rejected", slowGesture)
    }
    
    @Test
    fun testConflictDetection() {
        val manager = WidgetGestureManager.getInstance()
        
        // Strong upward swipe might conflict with system gestures
        val upwardGesture = WidgetGestureManager.GestureEvent(
            direction = WidgetGestureManager.SwipeDirection.UP,
            distance = 300f,
            velocity = 250f,
            duration = 150L,
            confidence = 0.9f
        )
        
        val hasConflict = manager.isGestureConflicting(mockContext, upwardGesture)
        assertTrue("Strong upward gesture should have conflict potential", hasConflict)
        
        // Horizontal swipes should not conflict
        val horizontalGesture = WidgetGestureManager.GestureEvent(
            direction = WidgetGestureManager.SwipeDirection.LEFT,
            distance = 150f,
            velocity = 120f,
            duration = 200L,
            confidence = 0.8f
        )
        
        val horizontalConflict = manager.isGestureConflicting(mockContext, horizontalGesture)
        assertFalse("Horizontal gesture should not conflict", horizontalConflict)
    }
}
