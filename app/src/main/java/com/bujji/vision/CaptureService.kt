package com.bujji.vision

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.util.DisplayMetrics
import androidx.core.app.NotificationCompat
import java.io.ByteArrayOutputStream
import java.net.ServerSocket
import kotlin.concurrent.thread

class CaptureService : Service() {

    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var latestFrame: ByteArray? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val resultCode = intent!!.getIntExtra("code", -1)
        val data: Intent = intent.getParcelableExtra("data")!!

        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = manager.getMediaProjection(resultCode, data)

        startForeground(1, notification())
        startCapture()
        startServer()

        return START_STICKY
    }

    private fun notification(): Notification {
        val channelId = "bujji_vision"

        val channel = NotificationChannel(
            channelId,
            "Bujji Vision",
            NotificationManager.IMPORTANCE_LOW
        )

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Bujji Vision Active")
            .setContentText("AI can see the screen")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()
    }

    private fun startCapture() {

        val metrics = resources.displayMetrics

        reader = ImageReader.newInstance(
            metrics.widthPixels,
            metrics.heightPixels,
            PixelFormat.RGBA_8888,
            2
        )

        projection!!.createVirtualDisplay(
            "bujji",
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader!!.surface,
            null,
            null
        )

        thread {
            while (true) {
                val image = reader!!.acquireLatestImage() ?: continue
                val buffer = image.planes[0].buffer

                val bmp = android.graphics.Bitmap.createBitmap(
                    image.width,
                    image.height,
                    android.graphics.Bitmap.Config.ARGB_8888
                )
                bmp.copyPixelsFromBuffer(buffer)

                val stream = ByteArrayOutputStream()
                bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 70, stream)
                latestFrame = stream.toByteArray()

                image.close()
            }
        }
    }

    private fun startServer() {
        thread {
            val server = ServerSocket(8765)
            while (true) {
                val client = server.accept()
                val out = client.getOutputStream()
                val frame = latestFrame ?: ByteArray(0)

                out.write(("HTTP/1.1 200 OK\r\nContent-Type: image/png\r\nContent-Length: ${frame.size}\r\n\r\n").toByteArray())
                out.write(frame)
                out.flush()
                client.close()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}