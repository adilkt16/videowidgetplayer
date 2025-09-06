package com.videowidgetplayer.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.videowidgetplayer.databinding.ActivityVideoWidgetConfigureBinding

class VideoWidgetConfigureActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityVideoWidgetConfigureBinding
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set the result to CANCELED in case the user backs out
        setResult(RESULT_CANCELED)
        
        binding = ActivityVideoWidgetConfigureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Get the widget ID from the intent
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }
        
        // If the intent doesn't contain a valid widget ID, finish the activity
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        
        setupUI()
    }
    
    private fun setupUI() {
        binding.addButton.setOnClickListener {
            val context = this@VideoWidgetConfigureActivity
            
            // Configure the widget
            val appWidgetManager = AppWidgetManager.getInstance(context)
            VideoWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetId)
            
            // Return success result
            val resultValue = Intent()
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(RESULT_OK, resultValue)
            finish()
        }
        
        binding.selectVideoButton.setOnClickListener {
            // Open video selection dialog
            selectVideoForWidget()
        }
    }
    
    private fun selectVideoForWidget() {
        // Implement video selection logic
        // This could open a file picker or show a list of available videos
    }
}
