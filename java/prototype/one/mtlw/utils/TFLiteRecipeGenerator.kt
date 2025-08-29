package prototype.one.mtlw.utils

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TFLiteRecipeGenerator(private val context: Context) {
    private var interpreter: Interpreter? = null

    fun loadModel(modelName: String = "1.tflite") {
        try {
            if (interpreter == null) {
                interpreter = Interpreter(loadModelFile(modelName))
                println("[DEBUG] Model loaded successfully")
            }
        } catch (e: Exception) {
            println("[ERROR] Failed to load model: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun loadModelFile(modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun getRecipeIndex(input: FloatArray): Int {
        try {
            // Get model input details
            val inputTensor = interpreter?.getInputTensor(0)
            val inputShape = inputTensor?.shape() ?: intArrayOf(1, input.size)
            val inputSize = inputShape.getOrNull(1) ?: input.size
            val output = IntArray(1)

            // Validate input
            if (input.size != inputSize) {
                throw IllegalArgumentException("Input size mismatch. Expected: $inputSize, Got: ${input.size}")
            }

            // Prepare input array
            val inputArray = Array(1) { input }

            // Run inference
            interpreter?.run(inputArray, output)
            
            println("[DEBUG] Model inference successful. Output: ${output[0]}")
            return output[0]
        } catch (e: Exception) {
            println("[ERROR] Model inference failed: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    fun getInputSize(): Int? {
        return try {
            interpreter?.getInputTensor(0)?.shape()?.getOrNull(1)
        } catch (e: Exception) {
            println("[ERROR] Failed to get input size: ${e.message}")
            null
        }
    }

    fun close() {
        try {
            interpreter?.close()
            interpreter = null
            println("[DEBUG] Model closed successfully")
        } catch (e: Exception) {
            println("[ERROR] Failed to close model: ${e.message}")
        }
    }
}

