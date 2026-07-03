package com.example.remotecontrol

import android.app.*
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import org.json.JSONObject
import org.webrtc.*

/**
 * Foreground Service مسؤولة عن:
 *  - تشغيل MediaProjection لالتقاط الشاشة
 *  - تحويل الالتقاط لـ VideoTrack وبثه عن طريق WebRTC PeerConnection
 *  - استقبال أوامر التحكم (tap/swipe/text) من الـ DataChannel وتنفيذها
 *    عن طريق RemoteControlAccessibilityService
 *
 * ملحوظة: أندرويد بيلزمك تعرض notification دايم وواضح طول ما فيه
 * بث شاشة شغال - ده مش اختياري، وده بالظبط اللي بيحمي خصوصية المستخدم
 * (بمعنى إنه دايمًا عارف إن فيه حد شايف شاشته).
 */
class ScreenCaptureService : Service() {

    private lateinit var webRtcManager: WebRtcManager
    private lateinit var signalingClient: SignalingClient
    private var mediaProjection: MediaProjection? = null

    companion object {
        const val CHANNEL_ID = "screen_capture_channel"
        const val NOTIFICATION_ID = 1
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())

        val resultCode = intent?.getIntExtra("resultCode", 0) ?: 0
        val data = intent?.getParcelableExtra<Intent>("data")

        if (data != null) {
            val projectionManager =
                getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)

            webRtcManager = WebRtcManager(applicationContext, mediaProjection!!) { command ->
                // أمر جه من جهاز التحكم -> نفّذه عن طريق Accessibility Service
                RemoteControlAccessibilityService.instance?.executeCommand(command)
            }

            signalingClient = SignalingClient(
                serverUrl = "ws:192.168.1.103:8080",
                onCodeReceived = {},
                onControllerJoined = {}
            )
            signalingClient.setOnSignalReceived { data2 -> webRtcManager.handleSignal(data2) }
            webRtcManager.onLocalSignal = { data2 -> signalingClient.sendSignal(data2) }

            webRtcManager.startCaptureAndOffer()
        }

        return START_STICKY
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "مشاركة الشاشة", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("التحكم عن بعد شغال")
            .setContentText("جهازك متاح للتحكم عن بعد دلوقتي")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaProjection?.stop()
        if (::webRtcManager.isInitialized) webRtcManager.dispose()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
