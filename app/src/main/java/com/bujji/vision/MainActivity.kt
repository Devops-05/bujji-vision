package com.bujji.vision

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle

class MainActivity : Activity() {

    private lateinit var manager: MediaProjectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(manager.createScreenCaptureIntent(), 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1 && resultCode == RESULT_OK) {

            val intent = Intent(this, CaptureService::class.java)
            intent.putExtra("code", resultCode)
            intent.putExtra("data", data)

            startForegroundService(intent)
            finish()
        }
    }
}