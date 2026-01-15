package com.metalens.app.settings

import android.content.Context
import com.meta.wearable.dat.camera.types.VideoQuality
import com.metalens.app.BuildConfig
import com.metalens.app.conversation.OpenAIRealtimeClient

object AppSettings {
    private const val PREFS_NAME = "meta_lens_ai_settings"
    private const val KEY_OPENAI_API_KEY = "openai_api_key"
    private const val KEY_OPENAI_MODEL = "openai_model"
    private const val KEY_CAMERA_VIDEO_QUALITY = "camera_video_quality"

    fun getOpenAiApiKey(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val fromPrefs = prefs.getString(KEY_OPENAI_API_KEY, null)
        if (!fromPrefs.isNullOrBlank()) return fromPrefs
        return BuildConfig.OPENAI_API_KEY
    }

    fun setOpenAiApiKey(context: Context, apiKey: String) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_OPENAI_API_KEY, apiKey.trim()).apply()
    }

    fun getOpenAiModel(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val fromPrefs = prefs.getString(KEY_OPENAI_MODEL, null)
        if (!fromPrefs.isNullOrBlank()) return fromPrefs
        val fromBuild = BuildConfig.OPENAI_MODEL
        if (fromBuild.isNotBlank()) return fromBuild
        return OpenAIRealtimeClient.DEFAULT_MODEL
    }

    fun setOpenAiModel(context: Context, model: String) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_OPENAI_MODEL, model.trim()).apply()
    }

    fun getCameraVideoQuality(context: Context): VideoQuality {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_CAMERA_VIDEO_QUALITY, null)?.trim()?.uppercase()
        return when (raw) {
            "LOW" -> VideoQuality.LOW
            "HIGH" -> VideoQuality.HIGH
            "MEDIUM", null, "" -> VideoQuality.MEDIUM
            else -> VideoQuality.MEDIUM
        }
    }

    fun setCameraVideoQuality(context: Context, quality: VideoQuality) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CAMERA_VIDEO_QUALITY, quality.name).apply()
    }
}

