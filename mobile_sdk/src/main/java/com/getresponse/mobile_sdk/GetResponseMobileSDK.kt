@file:Suppress("DEPRECATION")

package com.getresponse.mobile_sdk

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import com.getresponse.mobile_sdk.GetResponseMobileSdkClient.getApi
import com.getresponse.mobile_sdk.GetResponseMobileSdkClient.getStatsApi
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID


class GetResponseMobileSDK(
    private val context: Context,
    private val applicationId: String,
    private val entryPoint: String,
    private val secretKey: String,
    @DrawableRes private val notificationIcon: Int,
    private val channelsConfig: List<AndroidChannel> = emptyList(),
    private val enableDebug: Boolean = false,
) {
    private val installationUUID: String by lazy {
        var installationUID = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
            .getString(INSTALLATION_UUID_KEY, null)

        if (installationUID == null) {
            installationUID = UUID.randomUUID().toString().lowercase()
            context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
                .edit()
                .putString(INSTALLATION_UUID_KEY, installationUID)
                .apply()
        }
        installationUID
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    suspend fun consent(lang: String, externalId: String, email: String?, fcmToken: String) {
        getApi(entryPoint, createJWTToken(), enableDebug).consent(ConsentModel(lang, externalId, email, fcmToken))
    }

    suspend fun deleteConsent() {
        getApi(entryPoint, createJWTToken(), enableDebug).consentDelete()
    }

    fun handleIncomingPush(data: MutableMap<String, String>, activityClass: Class<*>): Boolean {
        // Check if message contains a GR notification payload.
        if (data["issuer"] != "getresponse") {
            Log.d(TAG, "Not a GR notification")
            return false
        }

        val intent = Intent(context, activityClass).apply {
            putExtra(DATA_KEY_IN_INTENT, HashMap(data))
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            SystemClock.uptimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = data.getOrDefault("channel_id", DEFAULT_CHANNEL_ID)

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(data["title"])
            .setContentText(data["body"])
            .setSmallIcon(notificationIcon)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (data.containsKey("image")) {
            applyImageUrl(notificationBuilder, data["image"]!!)
        }

        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelsConfig.forEach {
                if (notificationManager.getNotificationChannel(it.id) == null) {
                    notificationManager.createNotificationChannel(
                        NotificationChannel(it.id, it.name, it.importance)
                    )
                }
            }
            // Fallback if channel is missing:
            if (notificationManager.getNotificationChannel(channelId) == null) {
                Log.i(TAG, "Notification channel missing for channelId: $channelId, Creating default channel")
                notificationManager.createNotificationChannel(
                    NotificationChannel(channelId, DEFAULT_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
                )
            }
        }
        scope.launch {
            data["stats_url"]?.let {
                getStatsApi(enableDebug).stats(EventType.SHOWED.getEventUrl(it))
            }
        }
        notificationManager.notify(SystemClock.uptimeMillis().toInt(), notificationBuilder.build())
        return true
    }

    fun handleIncomingNotification(context: Context, intent: Intent?): Map<String, String>? {
        var data: Map<String, String>?
        var isLocal = true
        val intentWithData = intent ?: return null
        data = getDataFromNotification(intentWithData)
        if (data == null) {
            isLocal = false
            data = intent.extras?.let { extras ->
                val map = mutableMapOf<String, String>()
                extras.keySet().forEach { key ->
                    map[key] = extras[key].toString()
                }
                map
            }
        }
        if (data == null || data["issuer"] != "getresponse") {
            Log.e(TAG, "No data in notification")
            return null
        }
        if (data["issuer"] != "getresponse") {
            Log.e(TAG, "No GetResponse notification")
            return null
        }
        if (data["redirect_type"] == "url" && data.containsKey("redirect_destination")) {
            val i = Intent(Intent.ACTION_VIEW)
            i.setData(Uri.parse(data["redirect_destination"]))
            context.startActivity(i)
        }
        scope.launch {
            if (!isLocal) {
                data["stats_url"]?.let {
                    getStatsApi(enableDebug).stats(EventType.SHOWED.getEventUrl(it))
                }
            }
            data["stats_url"]?.let {
                getStatsApi(enableDebug).stats(EventType.CLICKED.getEventUrl(it))
            }
        }
        return data.filter { it.key !in listOf("stats_url", "issuer", "channel_id", "title", "body", "image") }
    }

    private fun createJWTToken() = Jwts.builder()
        .issuer(applicationId)
        .issuedAt(Date().apply { time -= 1000 })
        .expiration(Date().apply { time += 19000 })
        .setAudience(installationUUID)
        .signWith(
            SignatureAlgorithm.HS256,
            secretKey.toByteArray()
        )
        .compact()

    companion object {
        private const val PREFS_KEY = "getresponse_mobile_sdk"
        private const val INSTALLATION_UUID_KEY = "installation_uuid"
        private const val TAG = "GetResponseMobileSDK"
        private const val DATA_KEY_IN_INTENT = "getresponse_mobile_sdk_data"
        private const val DEFAULT_CHANNEL_ID = "default"
        private const val DEFAULT_CHANNEL_NAME = "Default Channel"

        @Suppress("UNCHECKED_CAST")
        private fun getDataFromNotification(intent: Intent): Map<String, String>? =
            (intent.getSerializableExtra(DATA_KEY_IN_INTENT) as HashMap<String, String>?)?.toMap()
    }
}