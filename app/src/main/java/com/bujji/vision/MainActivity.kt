package com.bujji.vision

import android.app.*
import android.content.*
import android.graphics.*
import android.hardware.display.*
import android.media.*
import android.media.projection.*
import android.os.*
import android.util.DisplayMetrics
import java.io.*
import java.net.ServerSocket
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private lateinit var projectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var latestFrame: ByteArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        projectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        startActivityForResult(
            projectionManager.createScreenCaptureIntent(),
            1
        )
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            mediaProjection =
                projectionManager.getMediaProjection(resultCode, data!!)

            startCapture()
            startServer()
        }
    }

    private fun startCapture() {
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)

        imageReader = ImageReader.newInstance(
            metrics.widthPixels,
            metrics.heightPixels,
            PixelFormat.RGBA_8888,
            2
        )

        mediaProjection!!.createVirtualDisplay(
            "bujji",
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface,
            null,
            null
        )

        thread {
            while (true) {
                val image = imageReader!!.acquireLatestImage() ?: continue
                val plane = image.planes[0]
                val buffer = plane.buffer

                val bitmap = Bitmap.createBitmap(
                    image.width,
                    image.height,
                    Bitmap.Config.ARGB_8888
                )

                bitmap.copyPixelsFromBuffer(buffer)

                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 70, stream)
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

                out.write(
                    ("HTTP/1.1 200 OK\r\n" +
                     "Content-Type: image/png\r\n" +
                     "Content-Length: ${frame.size}\r\n\r\n").toByteArray()
                )

                out.write(frame)
                out.flush()
                client.close()
            }
        }
    }
}