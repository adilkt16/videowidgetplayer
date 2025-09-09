package com.videowidgetplayer.utils

import android.util.Log
import java.lang.ref.WeakReference

/**
 * Memory leak detection utility for video widget services
 */
object MemoryLeakDetector {
    
    private const val TAG = "MemoryLeakDetector"
    private val trackedObjects = mutableMapOf<String, WeakReference<Any>>()
    
    fun trackObject(name: String, obj: Any) {
        trackedObjects[name] = WeakReference(obj)
        Log.d(TAG, "Tracking object: $name")
    }
    
    fun releaseObject(name: String) {
        trackedObjects.remove(name)
        Log.d(TAG, "Released object: $name")
    }
    
    fun checkForLeaks(): Int {
        Log.d(TAG, "=== Memory Leak Check ===")
        var leakCount = 0
        
        val iterator = trackedObjects.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val obj = entry.value.get()
            
            if (obj == null) {
                // Object was garbage collected - good
                iterator.remove()
                Log.d(TAG, "✓ ${entry.key} was garbage collected")
            } else {
                // Object still exists - potential leak
                leakCount++
                Log.w(TAG, "⚠ POTENTIAL LEAK: ${entry.key} still in memory")
            }
        }
        
        if (leakCount == 0) {
            Log.d(TAG, "✓ No memory leaks detected")
        } else {
            Log.e(TAG, "❌ Found $leakCount potential memory leaks!")
        }
        
        Log.d(TAG, "=== End Memory Leak Check ===")
        return leakCount
    }
    
    fun forceGC() {
        Log.d(TAG, "Forcing garbage collection...")
        System.gc()
        
        // Wait a bit for GC to complete
        try {
            Thread.sleep(100)
        } catch (e: InterruptedException) {
            // Ignore
        }
    }
}
