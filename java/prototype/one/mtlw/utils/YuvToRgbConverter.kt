package prototype.one.mtlw.utils

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

class YuvToRgbConverter {
    fun toBitmap(image: ImageProxy): Bitmap {
        val width = image.width
        val height = image.height

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuffer: ByteBuffer = yPlane.buffer
        val uBuffer: ByteBuffer = uPlane.buffer
        val vBuffer: ByteBuffer = vPlane.buffer

        val yRowStride = yPlane.rowStride
        val uvRowStride = uPlane.rowStride
        val uvPixelStride = uPlane.pixelStride

        val out = IntArray(width * height)

        var outputOffset = 0
        for (row in 0 until height) {
            val yRow = yRowStride * row
            val uvRow = uvRowStride * (row shr 1)
            for (col in 0 until width) {
                val y = 0xFF and yBuffer.get(yRow + col).toInt()
                val uvCol = (col shr 1) * uvPixelStride
                val u = 0xFF and uBuffer.get(uvRow + uvCol).toInt()
                val v = 0xFF and vBuffer.get(uvRow + uvCol).toInt()

                val yClamped = (y - 16).coerceAtLeast(0)
                val uShifted = u - 128
                val vShifted = v - 128

                val y1192 = 1192 * yClamped
                var r = (y1192 + 1634 * vShifted) shr 10
                var g = (y1192 - 833 * vShifted - 400 * uShifted) shr 10
                var b = (y1192 + 2066 * uShifted) shr 10

                if (r < 0) r = 0 else if (r > 255) r = 255
                if (g < 0) g = 0 else if (g > 255) g = 255
                if (b < 0) b = 0 else if (b > 255) b = 255

                out[outputOffset++] = -0x1000000 or (r shl 16) or (g shl 8) or b
            }
        }

        return Bitmap.createBitmap(out, width, height, Bitmap.Config.ARGB_8888)
    }
}


