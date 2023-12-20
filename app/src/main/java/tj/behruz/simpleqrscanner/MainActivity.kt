package tj.behruz.simpleqrscanner

import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.OrientationEventListener
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import tj.behruz.common.ScanningResultListener
import tj.behruz.simpleqrscanner.ui.common.MLKitBarcodeAnalyzer
import tj.behruz.simpleqrscanner.databinding.QrScannerLayoutBinding
import tj.behruz.simpleqrscanner.ui.QrCodeAnalyzer
import tj.behruz.simpleqrscanner.ui.theme.SImpleQrScannerTheme
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalGetImage::class) override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current

            SImpleQrScannerTheme {
                AndroidView(factory = { ctx ->
                    val view = QrScannerLayoutBinding.inflate(layoutInflater,null,false)
                       view.root.apply {

                       }
                    view.overlay.post {
                        view.overlay.setViewFinder()
                    }
                    val cameraExecutor = Executors.newSingleThreadExecutor()

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                        try {
                            // Unbind use cases before rebinding
                            cameraProvider.unbindAll()

                            // Bind use cases to camera
                            val preview: Preview = Preview.Builder()
                                .build()

                            val cameraSelector: CameraSelector = CameraSelector.Builder()
                                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                                .build()

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setTargetResolution(Size(view.cameraPreview.width, view.cameraPreview.height))
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            val orientationEventListener = object : OrientationEventListener(context) {
                                override fun onOrientationChanged(orientation: Int) {
                                    // Monitors orientation values to determine the target rotation value
                                    val rotation: Int = when (orientation) {
                                        in 45..134 -> android.view.Surface.ROTATION_270
                                        in 135..224 -> android.view.Surface.ROTATION_180
                                        in 225..314 -> android.view.Surface.ROTATION_90
                                        else -> android.view.Surface.ROTATION_0
                                    }

                                    imageAnalysis.targetRotation = rotation
                                }
                            }
                            orientationEventListener.enable()
                            val callBack = object : ScanningResultListener{
                                override fun onScanned(result: String) {
                                    Log.d("TAG",result.toString())
                                }

                            }

                            val analyzer: ImageAnalysis.Analyzer = MLKitBarcodeAnalyzer(callBack)
                            imageAnalysis.setAnalyzer(cameraExecutor, analyzer)

                            preview.setSurfaceProvider(view.cameraPreview.surfaceProvider)
                            val camera = cameraProvider?.bindToLifecycle(this, cameraSelector, imageAnalysis, preview)


                        } catch (exc: Exception) {
                            Log.e("DEBUG", "Use case binding failed", exc)
                        }
                    }, ContextCompat.getMainExecutor(context))


                    // Use your view as usual...
                    view.root
                }, update = {

                })

            }
        }

    }


@Composable
@androidx.camera.core.ExperimentalGetImage
fun PreviewViewComposable() {

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            { context ->
                val cameraExecutor = Executors.newSingleThreadExecutor()
                val previewView = PreviewView(context).also {
                    it.scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                    val imageCapture = ImageCapture.Builder().build()

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor, QrCodeAnalyzer {
                                Toast.makeText(context, "Barcode found", Toast.LENGTH_SHORT).show()
                            })
                        }

                    val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                    try {
                        // Unbind use cases before rebinding
                        cameraProvider.unbindAll()

                        // Bind use cases to camera
                        cameraProvider.bindToLifecycle(
                            context as ComponentActivity,
                            cameraSelector,
                            preview,
                            imageCapture,
                            imageAnalyzer
                        )

                    } catch (exc: Exception) {
                        Log.e("DEBUG", "Use case binding failed", exc)
                    }
                }, ContextCompat.getMainExecutor(context))
                previewView
            },
        )
    }

}
}




