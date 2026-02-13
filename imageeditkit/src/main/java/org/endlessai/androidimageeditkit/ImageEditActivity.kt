package org.endlessai.androidimageeditkit

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface

class ImageEditActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialUri = intent.parcelableExtra<Uri>(ImageEditKit.EXTRA_INPUT_URI)

        setContent {
            MaterialTheme {
                Surface {
                    ImageEditScreen(
                        initialUri = initialUri,
                        onCancel = {
                            setResult(Activity.RESULT_CANCELED)
                            finish()
                        },
                        onConfirm = { result ->
                            finishWithResult(result)
                        }
                    )
                }
            }
        }
    }

    private fun finishWithResult(result: ImageEditResult) {
        val data = Intent().apply {
            putExtra(ImageEditKit.EXTRA_OUTPUT_URI, result.outputUri)
            putExtra(ImageEditKit.EXTRA_OUTPUT_WIDTH, result.outputWidth)
            putExtra(ImageEditKit.EXTRA_OUTPUT_HEIGHT, result.outputHeight)
            clipData = ClipData.newUri(contentResolver, "edited-image", result.outputUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        setResult(Activity.RESULT_OK, data)
        grantUriPermission(
            packageName,
            result.outputUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        finish()
    }
}
