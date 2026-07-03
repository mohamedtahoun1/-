package com.example.remotecontrol

import android.content.Context
import android.media.projection.MediaProjection
import org.json.JSONObject
import org.webrtc.*

/**
 * مسؤول عن كل تفاصيل WebRTC: إنشاء PeerConnection، بث الشاشة كـ VideoTrack،
 * واستقبال أوامر التحكم عن طريق DataChannel.
 */
class WebRtcManager(
    private val context: Context,
    private val mediaProjection: MediaProjection,
    private val onControlCommand: (JSONObject) -> Unit
) {
    var onLocalSignal: ((JSONObject) -> Unit)? = null

    private val eglBase = EglBase.create()
    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private lateinit var peerConnection: PeerConnection
    private lateinit var videoCapturer: VideoCapturer
    private lateinit var videoSource: VideoSource
    private lateinit var surfaceTextureHelper: SurfaceTextureHelper

    init {
        val initOptions = PeerConnectionFactory.InitializationOptions
            .builder(context)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(initOptions)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()
    }

    fun startCaptureAndOffer() {
        videoCapturer = ScreenCapturerAndroid(mediaProjection, object : MediaProjection.Callback() {})
        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
        videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast)
        videoCapturer.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)

        // دقة وFPS البث - عدّلهم حسب سرعة النت المتاحة
        videoCapturer.startCapture(1280, 720, 30)

        val videoTrack = peerConnectionFactory.createVideoTrack("screen_track", videoSource)

        val rtcConfig = PeerConnection.RTCConfiguration(
            listOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
        )

        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                onLocalSignal?.invoke(JSONObject().apply {
                    put("candidate", JSONObject().apply {
                        put("sdpMid", candidate.sdpMid)
                        put("sdpMLineIndex", candidate.sdpMLineIndex)
                        put("candidate", candidate.sdp)
                    })
                })
            }
            override fun onDataChannel(channel: DataChannel) {
                channel.registerObserver(object : DataChannel.Observer {
                    override fun onMessage(buffer: DataChannel.Buffer) {
                        val bytes = ByteArray(buffer.data.remaining())
                        buffer.data.get(bytes)
                        val json = JSONObject(String(bytes))
                        onControlCommand(json)
                    }
                    override fun onBufferedAmountChange(p0: Long) {}
                    override fun onStateChange() {}
                })
            }
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onAddStream(p0: MediaStream?) {}
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {}
        })!!

        peerConnection.addTrack(videoTrack, listOf("screen_stream"))

        val constraints = MediaConstraints()
        peerConnection.createOffer(object : SdpObserver by SdpObserverAdapter() {
            override fun onCreateSuccess(desc: SessionDescription) {
                peerConnection.setLocalDescription(SdpObserverAdapter(), desc)
                onLocalSignal?.invoke(JSONObject().apply {
                    put("sdp", JSONObject().apply {
                        put("type", desc.type.canonicalForm())
                        put("sdp", desc.description)
                    })
                })
            }
        }, constraints)
    }

    fun handleSignal(data: JSONObject) {
        if (data.has("sdp")) {
            val sdpJson = data.getJSONObject("sdp")
            val desc = SessionDescription(
                SessionDescription.Type.fromCanonicalForm(sdpJson.getString("type")),
                sdpJson.getString("sdp")
            )
            peerConnection.setRemoteDescription(SdpObserverAdapter(), desc)
        } else if (data.has("candidate")) {
            val c = data.getJSONObject("candidate")
            peerConnection.addIceCandidate(
                IceCandidate(c.getString("sdpMid"), c.getInt("sdpMLineIndex"), c.getString("candidate"))
            )
        }
    }

    fun dispose() {
        videoCapturer.stopCapture()
        peerConnection.close()
    }
}

/** Adapter بسيط عشان منضطرش نعمل override لكل methods كل مرة */
open class SdpObserverAdapter : SdpObserver {
    override fun onCreateSuccess(p0: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(p0: String?) {}
    override fun onSetFailure(p0: String?) {}
}
