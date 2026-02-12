package com.helloai.androidimageeditkit

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract

data class ImageEditResult(
    val outputUri: Uri,
    val outputWidth: Int,
    val outputHeight: Int
)

object ImageEditKit {
    const val EXTRA_INPUT_URI: String = "com.helloai.androidimageeditkit.extra.INPUT_URI"
    const val EXTRA_OUTPUT_URI: String = "com.helloai.androidimageeditkit.extra.OUTPUT_URI"
    const val EXTRA_OUTPUT_WIDTH: String = "com.helloai.androidimageeditkit.extra.OUTPUT_WIDTH"
    const val EXTRA_OUTPUT_HEIGHT: String = "com.helloai.androidimageeditkit.extra.OUTPUT_HEIGHT"

    fun createIntent(context: Context, inputImageUri: Uri? = null): Intent {
        return Intent(context, ImageEditActivity::class.java).apply {
            inputImageUri?.let { putExtra(EXTRA_INPUT_URI, it) }
        }
    }

    fun parseResult(resultCode: Int, data: Intent?): ImageEditResult? {
        if (resultCode != Activity.RESULT_OK || data == null) {
            return null
        }
        val uri = data.parcelableExtra<Uri>(EXTRA_OUTPUT_URI) ?: return null
        val width = data.getIntExtra(EXTRA_OUTPUT_WIDTH, -1)
        val height = data.getIntExtra(EXTRA_OUTPUT_HEIGHT, -1)
        if (width <= 0 || height <= 0) {
            return null
        }
        return ImageEditResult(
            outputUri = uri,
            outputWidth = width,
            outputHeight = height
        )
    }
}

class ImageEditContract : ActivityResultContract<Uri?, ImageEditResult?>() {
    override fun createIntent(context: Context, input: Uri?): Intent {
        return ImageEditKit.createIntent(context = context, inputImageUri = input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): ImageEditResult? {
        return ImageEditKit.parseResult(resultCode = resultCode, data = intent)
    }
}

internal inline fun <reified T> Intent.parcelableExtra(key: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(key)
    }
}
