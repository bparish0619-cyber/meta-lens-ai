package com.metalens.app.conversation

import android.util.Base64
import android.util.Log
import android.os.SystemClock
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Minimal OpenAI Realtime WebSocket client for audio-only conversation.
 *
 * Protocol refs (high level):
 * - Client audio in: `input_audio_buffer.append` with base64 PCM16
 * - Server audio out: `response.audio.delta` with base64 PCM16
 */
class OpenAIRealtimeClient(
    private val apiKey: String,
    private val model: String = DEFAULT_MODEL,
    private val voice: String = DEFAULT_VOICE,
    private val onConnected: () -> Unit,
    private val onDisconnected: (reason: String) -> Unit,
    private val onError: (message: String) -> Unit,
    private val onAudioDelta: (pcm16: ByteArray) -> Unit,
    private val onAssistantTextDelta: (delta: String) -> Unit,
    private val onUserUtteranceStarted: (itemId: String) -> Unit,
    private val onUserUtteranceStopped: (itemId: String, durationMs: Long) -> Unit,
    private val onUserTranscript: (itemId: String, transcript: String) -> Unit,
    private val onAssistantResponseStarted: (responseId: String?) -> Unit,
    private val onAssistantResponseDone: () -> Unit,
    private val onInputSpeechState: (isSpeeching: Boolean) -> Unit,
) {
    companion object {
        private const val TAG = "OpenAIRealtimeClient"
        const val DEFAULT_MODEL = "gpt-4o-mini-realtime-preview"
        const val DEFAULT_VOICE = "alloy"
        private const val REALTIME_URL = "wss://api.openai.com/v1/realtime"
    }

    private val client: OkHttpClient =
        OkHttpClient.Builder()
            // Keep the WS alive; the app controls lifecycle explicitly.
            .pingInterval(15, TimeUnit.SECONDS)
            .build()

    private var socket: WebSocket? = null
    private val httpStatusPattern: Pattern = Pattern.compile("\\b(\\d{3})\\b")
    private var activeResponseId: String? = null
    private var isResponseInProgress: Boolean = false
    private var pendingResponseCreate: Boolean = false
    private val speechStartByItemId = linkedMapOf<String, Long>()

    fun connect() {
        if (apiKey.isBlank()) {
            onError("Missing OpenAI API key (BuildConfig.OPENAI_API_KEY is empty)")
            return
        }

        Log.d(TAG, "Connecting to Realtime: model=$model voice=$voice")
        val request =
            Request.Builder()
                .url("$REALTIME_URL?model=$model")
                .addHeader("Authorization", "Bearer $apiKey")
                // Some deployments still expect this header; safe to include.
                .addHeader("OpenAI-Beta", "realtime=v1")
                .build()

        socket =
            client.newWebSocket(
                request,
                object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        Log.d(TAG, "WebSocket opened: code=${response.code}")
                        // Configure session for audio in/out, server VAD and a voice.
                        webSocket.send(buildSessionUpdate())
                        onConnected()
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        val type =
                            try {
                                JSONObject(text).optString("type")
                            } catch (_: Throwable) {
                                ""
                            }
                        Log.d(TAG, "onMessage type=$type raw=${text.take(300)}")
                        handleServerEvent(text)
                    }

                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                        Log.d(TAG, "WebSocket closing: code=$code reason=$reason")
                        webSocket.close(code, reason)
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        Log.d(TAG, "WebSocket closed: code=$code reason=$reason")
                        onDisconnected("closed($code): $reason")
                    }

                    override fun onFailure(
                        webSocket: WebSocket,
                        t: Throwable,
                        response: Response?,
                    ) {
                        Log.e(TAG, "WebSocket failure: ${response?.code} ${response?.message}", t)
                        onError(
                            formatErrorMessage(
                                message = t.message ?: "WebSocket failure",
                                httpStatus = response?.code,
                            ),
                        )
                        onDisconnected("failure")
                    }
                },
            )
    }

    fun close() {
        socket?.close(1000, "client close")
        socket = null
        activeResponseId = null
        isResponseInProgress = false
        pendingResponseCreate = false
        speechStartByItemId.clear()
    }

    fun sendPcm16Audio(pcm16: ByteArray) {
        val ws = socket ?: return
        if (pcm16.isEmpty()) return
        val payload =
            JSONObject()
                .put("type", "input_audio_buffer.append")
                .put("audio", Base64.encodeToString(pcm16, Base64.NO_WRAP))
                .toString()
        ws.send(payload)
    }

    fun sendUserText(text: String) {
        val ws = socket ?: return
        if (text.isBlank()) return

        // If the assistant is currently responding, cancel immediately.
        if (isResponseInProgress) {
            Log.d(TAG, "User sent text while responding -> cancelling active response")
            sendResponseCancel()
        }

        val item =
            JSONObject()
                .put("type", "message")
                .put("role", "user")
                .put(
                    "content",
                    org.json.JSONArray()
                        .put(
                            JSONObject()
                                .put("type", "input_text")
                                .put("text", text),
                        ),
                )

        ws.send(
            JSONObject()
                .put("type", "conversation.item.create")
                .put("item", item)
                .toString(),
        )

        // Trigger a new response (defer if one is still in progress).
        if (isResponseInProgress) {
            pendingResponseCreate = true
        } else {
            sendResponseCreate()
        }
    }

    /**
     * Cancel any active assistant response (best-effort).
     *
     * Safe to call even if nothing is active; harmless server errors are ignored in `error` handling.
     */
    fun cancelActiveResponse() {
        sendResponseCancel()
    }

    private fun sendResponseCreate() {
        val ws = socket ?: return
        val response =
            JSONObject()
                .put("modalities", org.json.JSONArray().put("audio").put("text"))
        val payload =
            JSONObject()
                .put("type", "response.create")
                .put("response", response)
                .toString()
        ws.send(payload)
    }

    private fun sendResponseCancel() {
        val ws = socket ?: return
        val payload =
            JSONObject()
                .put("type", "response.cancel")
                .apply {
                    // If we know the active response id, include it (some servers accept optional id).
                    activeResponseId?.let { put("response_id", it) }
                }
                .toString()
        ws.send(payload)
    }

    private fun buildSessionUpdate(): String {
        val session =
            JSONObject()
                .put("modalities", org.json.JSONArray().put("audio").put("text"))
                .put("input_audio_format", "pcm16")
                .put("output_audio_format", "pcm16")
                .put("voice", voice)
                // We manually trigger `response.create` on speech end for reliability.
                .put(
                    "turn_detection",
                    JSONObject()
                        .put("type", "server_vad")
                        .put("create_response", false)
                        // Server-assisted interruption when the user starts speaking.
                        .put("interrupt_response", true),
                )
                // Enable transcription so we can show user speech in the UI.
                .put(
                    "input_audio_transcription",
                    JSONObject()
                        .put("model", "whisper-1"),
                )

        return JSONObject()
            .put("type", "session.update")
            .put("session", session)
            .toString()
    }

    private fun handleServerEvent(text: String) {
        val obj =
            try {
                JSONObject(text)
            } catch (_: Throwable) {
                return
            }

        when (val type = obj.optString("type")) {
            "response.audio.delta",
            "response.output_audio.delta",
            -> {
                if (!isResponseInProgress) isResponseInProgress = true
                val b64 = obj.optString("delta")
                if (b64.isNullOrBlank()) return
                val bytes =
                    try {
                        Base64.decode(b64, Base64.DEFAULT)
                    } catch (_: Throwable) {
                        return
                    }
                onAudioDelta(bytes)
            }
            // Text transcript of assistant audio (streaming).
            "response.audio_transcript.delta" -> {
                if (!isResponseInProgress) isResponseInProgress = true
                val delta = obj.optString("delta")
                if (!delta.isNullOrBlank()) onAssistantTextDelta(delta)
            }
            "response.audio_transcript.done" -> {
                if (!isResponseInProgress) isResponseInProgress = true
                val transcript = obj.optString("transcript")
                if (!transcript.isNullOrBlank()) onAssistantTextDelta(transcript)
            }
            "response.text.delta" -> {
                val delta = obj.optString("delta")
                if (!delta.isNullOrBlank()) onAssistantTextDelta(delta)
            }
            // Some servers emit `response.output_text.delta` instead of `response.text.delta`.
            "response.output_text.delta" -> {
                val delta = obj.optString("delta")
                if (!delta.isNullOrBlank()) onAssistantTextDelta(delta)
            }
            // User speech detection.
            "input_audio_buffer.speech_started" -> {
                onInputSpeechState(true)
                obj.optString("item_id")?.takeIf { it.isNotBlank() }?.let { itemId ->
                    speechStartByItemId[itemId] = SystemClock.elapsedRealtime()
                    onUserUtteranceStarted(itemId)
                }
                // Cancel only when a response is actually active.
                if (isResponseInProgress) {
                    Log.d(TAG, "User started speaking -> cancelling active response")
                    sendResponseCancel()
                }
            }
            "input_audio_buffer.speech_stopped" -> {
                onInputSpeechState(false)
                obj.optString("item_id")?.takeIf { it.isNotBlank() }?.let { itemId ->
                    val start = speechStartByItemId.remove(itemId)
                    val duration =
                        if (start != null) SystemClock.elapsedRealtime() - start else 0L
                    onUserUtteranceStopped(itemId, duration)
                }
                // With server VAD enabled, the server commits automatically (we see
                // `input_audio_buffer.committed`). Sending our own commit can race and produce
                // `input_audio_buffer_commit_empty`. So we only request a response here.
                if (isResponseInProgress) {
                    // If a response is still in flight (or cancel is still processing),
                    // defer creation until we get response.done / response.cancelled.
                    pendingResponseCreate = true
                    Log.d(TAG, "Deferring response.create (response still in progress)")
                } else {
                    sendResponseCreate()
                }
            }
            // User transcription (async)
            "conversation.item.input_audio_transcription.completed" -> {
                val transcript = obj.optString("transcript")
                val itemId = obj.optString("item_id")
                if (!itemId.isNullOrBlank() && !transcript.isNullOrBlank()) {
                    onUserTranscript(itemId, transcript)
                }
            }
            "conversation.item.input_audio_transcription.failed" -> {
                val err = obj.optJSONObject("error")
                val msg = err?.optString("message") ?: "Input transcription failed"
                onError(formatErrorMessage(message = msg))
            }
            "error" -> {
                val err = obj.optJSONObject("error")
                val code = err?.optString("code")
                val msg =
                    err?.optString("message")
                        ?: obj.optString("message")
                        ?: "Unknown Realtime error"
                // Ignore commit-empty races (server VAD already committed).
                if (code == "input_audio_buffer_commit_empty") {
                    Log.d(TAG, "Ignoring commit-empty error: $msg")
                    return
                }
                // We aggressively cancel on speech start; if there's nothing to cancel,
                // the server can respond with `cancel_not_active`. That's harmless.
                if (code == "cancel_not_active") {
                    Log.d(TAG, "Ignoring cancel_not_active: $msg")
                    return
                }
                // Some servers may return a cancellation failure message without a code.
                if (!isResponseInProgress && msg.contains("cancellation failed", ignoreCase = true)) {
                    Log.d(TAG, "Ignoring cancellation failed (no active response): $msg")
                    return
                }
                onError(formatErrorMessage(message = msg, code = code))
            }
            "response.created" -> {
                val response = obj.optJSONObject("response")
                activeResponseId = response?.optString("id") ?: activeResponseId
                isResponseInProgress = true
                onAssistantResponseStarted(activeResponseId)
            }
            "response.done" -> {
                val response = obj.optJSONObject("response")
                val status = response?.optString("status")
                // Any response.done means this response is no longer in progress.
                val doneId = response?.optString("id")
                if (!doneId.isNullOrBlank() && doneId == activeResponseId) {
                    activeResponseId = null
                }
                isResponseInProgress = false

                if (status == "failed") {
                    val details = response.optJSONObject("status_details")
                    val err = details?.optJSONObject("error")
                    val code = err?.optString("code")
                    val msg =
                        err?.optString("message")
                            ?: "Response failed"
                    onError(formatErrorMessage(message = msg, code = code))
                    onAssistantResponseDone()
                } else {
                    onAssistantResponseDone()
                }
                if (pendingResponseCreate) {
                    pendingResponseCreate = false
                    Log.d(TAG, "Sending deferred response.create after response.done")
                    sendResponseCreate()
                }
            }
            "response.cancelled" -> {
                // Some servers explicitly emit a cancelled event.
                activeResponseId = null
                isResponseInProgress = false
                onAssistantResponseDone()
                if (pendingResponseCreate) {
                    pendingResponseCreate = false
                    Log.d(TAG, "Sending deferred response.create after response.cancelled")
                    sendResponseCreate()
                }
            }
            else -> {
                // Keep noisy logs out of UI; logcat already captures the raw event.
                Log.d(TAG, "Unhandled event type=$type")
            }
        }
    }

    private fun formatErrorMessage(
        message: String,
        code: String? = null,
        httpStatus: Int? = null,
    ): String {
        val detectedHttpStatus =
            httpStatus
                ?: run {
                    val m = httpStatusPattern.matcher(message)
                    if (m.find()) m.group(1)?.toIntOrNull() else null
                }

        val parts = mutableListOf<String>()
        if (code != null && code.isNotBlank()) parts.add("code=$code")
        if (detectedHttpStatus != null) parts.add("http=$detectedHttpStatus")

        return if (parts.isEmpty()) {
            message
        } else {
            "[${parts.joinToString(", ")}] $message"
        }
    }
}

