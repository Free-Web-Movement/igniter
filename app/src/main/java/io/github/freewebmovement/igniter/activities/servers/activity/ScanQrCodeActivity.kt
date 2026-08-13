package io.github.freewebmovement.igniter.activities.servers.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import io.github.freewebmovement.igniter.R
import io.github.freewebmovement.igniter.theme.IgniterTheme
import io.github.freewebmovement.igniter.ui.scanqr.ScanQrScreen
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Full-screen QR scanner. Decodes a trojan:// URI and returns it to the caller.
 */
class ScanQrCodeActivity : AppCompatActivity() {

    private var previewView: PreviewView? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            Toast.makeText(this, R.string.qr_scan_permission_denied, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IgniterTheme {
                ScanQrScreen(onPreviewReady = { previewView = it })
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
        ProcessCameraProvider.getInstance(this).addListener({
            val provider = try {
                ProcessCameraProvider.getInstance(this).get()
            } catch (e: Exception) {
                Toast.makeText(this, R.string.qr_scan_camera_error, Toast.LENGTH_LONG).show()
                finish()
                return@addListener
            }
            val view = previewView ?: return@addListener
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(view.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage == null) {
                    imageProxy.close()
                    return@setAnalyzer
                }
                val inputImage = InputImage.fromMediaImage(
                    mediaImage, imageProxy.imageInfo.rotationDegrees)
                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        val text = barcodes.firstOrNull()?.rawValue
                        if (!text.isNullOrEmpty()) {
                            setResult(RESULT_OK, Intent().putExtra(EXTRA_QR_RESULT, text))
                            finish()
                        }
                    }
                    .addOnCompleteListener { imageProxy.close() }
            }
            try {
                provider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            } catch (e: Exception) {
                Toast.makeText(this, R.string.qr_scan_camera_error, Toast.LENGTH_LONG).show()
                finish()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        const val EXTRA_QR_RESULT = "qr_result"
    }
}
