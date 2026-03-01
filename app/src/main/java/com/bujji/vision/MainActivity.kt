package com.bujji.vision

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var manager: MediaProjectionManager
    private var requested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // simple visible UI (prevents immediate kill)
        val tv = TextView(this)
        tv.text = "Starting Bujji Vision..."
        tv.textSize = 20f
        setContentView(tv)

        manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    override fun onPostResume() {
        super.onPostResume()

        if (!requested) {
            requested = true
            startActivityForResult(manager.createScreenCaptureIntent(), 1)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {

            val intent = Intent(this, CaptureService::class.java)
            intent.putExtra("code", resultCode)
            intent.putExtra("data", data)

            startForegroundService(intent)
            finish()
        } else {
            finish()
        }
    }
}