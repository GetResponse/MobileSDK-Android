package com.getresponse.mobile_sdk.push_notifications

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
import com.getresponse.mobile_sdk.applyImageUrl
import com.getresponse.mobile_sdk.createJWTToken
import com.getresponse.mobile_sdk.models.push_notifications.AndroidChannel
import com.getresponse.mobile_sdk.models.push_notifications.ConsentModel
import com.getresponse.mobile_sdk.models.push_notifications.EventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray

class GetResponsePushNotificationsService(
    private val context: Context,
    private var isAvailable: Boolean = false,
    private var isEnabled: Boolean = false
) {
    private lateinit var applicationId: String
    private lateinit var entryPoint: String
    private lateinit var secretKey: String

    @DrawableRes
    private var notificationIcon: Int? = null
    private var channelsConfig: List<AndroidChannel> = emptyList()
    private val enableDebug: Boolean = false
    private lateinit var installationUUID: String

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    internal fun configure(
        applicationId: String,
        entryPoint: String,
        secretKey: String,
        notificationIcon: Int?,
        channelsConfig: List<AndroidChannel>,
        installationUUID: String,
        enableDebug: Boolean,
        isAvailable: Boolean
    ) {
        this.applicationId = applicationId
        this.entryPoint = entryPoint
        this.secretKey = secretKey
        this.notificationIcon = notificationIcon
        this.channelsConfig = channelsConfig
        this.installationUUID = installationUUID
        this.isAvailable = isAvailable
        isEnabled = true
    }

    suspend fun consent(lang: String, externalId: String, email: String?, fcmToken: String) {
        getApi(entryPoint, createJWTToken(applicationId, secretKey, installationUUID), enableDebug).consent(
            ConsentModel(
                lang,
                externalId,
                email,
                fcmToken
            )
        )
    }

    suspend fun deleteConsent() {
        getApi(entryPoint, createJWTToken(applicationId, secretKey, installationUUID), enableDebug).consentDelete()
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
            .setSmallIcon(notificationIcon ?: android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (data.containsKey("actions")) {
            val actionsJson = JSONArray(data["actions"])
            for (i in 0 until actionsJson.length()) {
                val actionObject = actionsJson.getJSONObject(i)
                val actionMap = mutableMapOf<String, String>()
                val keys = actionObject.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    actionMap[key] = actionObject.optString(key, "")
                }
                val text = actionObject.getString("text")

                val actionIntent = Intent(context, activityClass).apply {
                    putExtra(DATA_KEY_IN_INTENT, HashMap(data))
                    putExtra(ACTION_KEY_IN_INTENT, HashMap(actionMap))
                }

                val actionPendingIntent = PendingIntent.getActivity(
                    context,
                    SystemClock.uptimeMillis().toInt(),
                    actionIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                notificationBuilder.addAction(NotificationCompat.Action(0, text, actionPendingIntent))
            }
        }

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
        var actionData: Map<String, String>?
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

        actionData = getActionDataFromNotification(intentWithData)
        if (actionData == null) {
            isLocal = false
            actionData = intent.extras?.let { extras ->
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
        return data.filter { it.key !in listOf("stats_url", "issuer", "channel_id", "title", "body", "image") } + (actionData ?: emptyMap())
    }

    companion object {
        private const val TAG = "GetResponseMobileSDK"
        private const val DATA_KEY_IN_INTENT = "getresponse_mobile_sdk_data"
        private const val ACTION_KEY_IN_INTENT = "getresponse_mobile_sdk_action_data"
        private const val DEFAULT_CHANNEL_ID = "default"
        private const val DEFAULT_CHANNEL_NAME = "Default Channel"

        @Suppress("UNCHECKED_CAST")
        private fun getDataFromNotification(intent: Intent): Map<String, String>? =
            (intent.getSerializableExtra(DATA_KEY_IN_INTENT) as HashMap<String, String>?)?.toMap()

        @Suppress("UNCHECKED_CAST")
        private fun getActionDataFromNotification(intent: Intent): Map<String, String>? =
            (intent.getSerializableExtra(ACTION_KEY_IN_INTENT) as HashMap<String, String>?)?.toMap()
    }
}