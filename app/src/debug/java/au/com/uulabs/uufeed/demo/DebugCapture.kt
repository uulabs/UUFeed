package au.com.uulabs.uufeed.demo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import java.io.File
import java.lang.ref.WeakReference

fun installDebugCapture(activity: ComponentActivity) {
    DebugCapture.activity = WeakReference(activity)
}

object DebugCapture {
    var activity: WeakReference<ComponentActivity>? = null

    fun capture(name: String): String {
        val activity = activity?.get() ?: return "no-activity"
        val view = activity.window.decorView
        val width = view.width
        val height = view.height
        if (width <= 0 || height <= 0) return "zero-size"

        val drawn = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(drawn))
        val drawFile = File(activity.getExternalFilesDir(null), "$name.png")
        drawFile.outputStream().use { drawn.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return "draw=${score(drawn)} ${drawFile.absolutePath}"
    }

    private fun score(bitmap: Bitmap): Int {
        var hits = 0
        val stepX = (bitmap.width / 24).coerceAtLeast(1)
        val stepY = (bitmap.height / 40).coerceAtLeast(1)
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                if ((pixel and 0x00FFFFFF) != 0) hits++
                x += stepX
            }
            y += stepY
        }
        return hits
    }
}

class DebugCaptureReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra("name") ?: "capture"
        // Drawing the window has to happen on the main thread.
        val message = DebugCapture.capture(name)
        setResultData(message)
    }
}
