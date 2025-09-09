package com.videowidgetplayer.ui

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.videowidgetplayer.R
import com.videowidgetplayer.service.SimpleWidgetVideoService

/**
 * Test activity to verify widget video service functionality
 */
class WidgetTestActivity : AppCompatActivity() {
    
    private val testWidgetId = 999 // Mock widget ID for testing
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget_test)
        
        setupTestButtons()
    }
    
    private fun setupTestButtons() {
        findViewById<Button>(R.id.testStartVideo).setOnClickListener {
            testStartVideo()
        }
        
        findViewById<Button>(R.id.testStopVideo).setOnClickListener {
            testStopVideo()
        }
        
        findViewById<Button>(R.id.testServiceStatus).setOnClickListener {
            testServiceStatus()
        }
    }
    
    private fun testStartVideo() {
        Log.d("WidgetTest", "Testing video start")
        
        try {
            // Use a simple test URI for now - replace with actual video
            val testVideoUri = "content://media/external/video/media/1"
            
            val intent = Intent(this, SimpleWidgetVideoService::class.java).apply {
                action = SimpleWidgetVideoService.ACTION_START_VIDEO
                putExtra(SimpleWidgetVideoService.EXTRA_WIDGET_ID, testWidgetId)
                putExtra(SimpleWidgetVideoService.EXTRA_VIDEO_URI, testVideoUri)
            }
            
            startService(intent)
            Toast.makeText(this, "Video service started", Toast.LENGTH_SHORT).show()
            Log.d("WidgetTest", "Video service start requested")
            
        } catch (e: Exception) {
            Log.e("WidgetTest", "Error starting video service", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun testStopVideo() {
        Log.d("WidgetTest", "Testing video stop")
        
        try {
            val intent = Intent(this, SimpleWidgetVideoService::class.java).apply {
                action = SimpleWidgetVideoService.ACTION_STOP_VIDEO
                putExtra(SimpleWidgetVideoService.EXTRA_WIDGET_ID, testWidgetId)
            }
            
            startService(intent)
            Toast.makeText(this, "Video service stopped", Toast.LENGTH_SHORT).show()
            Log.d("WidgetTest", "Video service stop requested")
            
        } catch (e: Exception) {
            Log.e("WidgetTest", "Error stopping video service", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun testServiceStatus() {
        Log.d("WidgetTest", "Testing service status")
        Toast.makeText(this, "Check logcat for service status", Toast.LENGTH_SHORT).show()
    }
}
