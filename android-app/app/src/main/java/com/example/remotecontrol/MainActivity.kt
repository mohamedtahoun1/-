package com.example.remotecontrol

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * الشاشة الرئيسية:
 * 1. بتوصل بسيرفر الـ signaling وتاخد كود اقتران (pairing code)
 * 2. بتوريك الكود عشان تدخله من جهاز التحكم
 * 3. بتطلب منك تفعّل Accessibility Service (مرة واحدة بس)
 * 4. لما جهاز تحكم يتصل، بتطلب صلاحية Screen Capture (dialog تلقائي من أندرويد)
 */
class MainActivity : AppCompatActivity() {

    private lateinit var codeTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var enableAccessibilityButton: Button

    private lateinit var signalingClient: SignalingClient
    private lateinit var mediaProjectionManager: MediaProjectionManager

    private val screenCaptureLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            // المستخدم وافق على مشاركة الشاشة -> ابدأ خدمة البث
            val intent = Intent(this, ScreenCaptureService::class.java).apply {
                putExtra("resultCode", result.resultCode)
                putExtra("data", result.data)
            }
            startForegroundService(intent)
            statusTextView.text = "البث شغال ✔"
        } else {
            statusTextView.text = "تم رفض إذن مشاركة الشاشة"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        codeTextView = findViewById(R.id.codeTextView)
        statusTextView = findViewById(R.id.statusTextView)
        enableAccessibilityButton = findViewById(R.id.enableAccessibilityButton)

        mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        enableAccessibilityButton.setOnClickListener {
            // بيوديك لصفحة الإعدادات عشان تفعل الخدمة يدويًا (خطوة أمان من أندرويد نفسه)
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        signalingClient = SignalingClient(
            serverUrl = "ws://192.168.1.103:8080", // غيّرها لعنوان سيرفرك
            onCodeReceived = { code ->
                runOnUiThread { codeTextView.text = code }
            },
            onControllerJoined = {
                runOnUiThread {
                    statusTextView.text = "جهاز تحكم اتصل، طالب إذن مشاركة الشاشة..."
                    // اطلب صلاحية التقاط الشاشة (dialog تلقائي)
                    screenCaptureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                }
            }
        )
        signalingClient.connect()
    }

    override fun onDestroy() {
        super.onDestroy()
        signalingClient.disconnect()
    }
}
