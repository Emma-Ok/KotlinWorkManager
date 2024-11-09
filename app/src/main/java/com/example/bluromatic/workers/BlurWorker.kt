package com.example.bluromatic
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.work.workDataOf
import com.example.bluromatic.workers.blurBitmap
import com.example.bluromatic.workers.makeStatusNotification
import com.example.bluromatic.workers.writeBitmapToFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val TAG = "BlurWorker"
class BlurWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val resourceUri = inputData.getString(KEY_IMAGE_URI)
        val blurLevel = inputData.getInt(KEY_BLUR_LEVEL, 1)

        return withContext(Dispatchers.IO) {
            return@withContext try {
                require(!resourceUri.isNullOrBlank()) {
                    val errorMessage =
                        applicationContext.resources.getString(R.string.invalid_input_uri)
                    Log.e(TAG, errorMessage)
                    errorMessage
                }
                // This is an utility function added to emulate slower work.
                delay(DELAY_TIME_MILLIS)
                val resolver = applicationContext.contentResolver

                val picture = BitmapFactory.decodeStream(
                    resolver.openInputStream(Uri.parse(resourceUri))
                )
                val output = blurBitmap(picture, blurLevel)
                // Write bitmap to a temp file
                val outputUri = writeBitmapToFile(applicationContext, output)
                makeStatusNotification(
                    "Output is $outputUri",
                    applicationContext
                )
                val outputData = workDataOf(KEY_IMAGE_URI to outputUri.toString())
                Result.success(outputData)
            } catch (throwable: Throwable) {
                Log.e(
                    TAG,
                    applicationContext.resources.getString(R.string.error_applying_blur),
                    throwable
                )
                Result.failure()
            }
        }
    }
}

