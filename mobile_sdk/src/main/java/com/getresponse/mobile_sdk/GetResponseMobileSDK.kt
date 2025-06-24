@file:Suppress("DEPRECATION")

package com.getresponse.mobile_sdk

import android.content.Context
import android.util.Log
import androidx.annotation.DrawableRes
import com.getresponse.mobile_sdk.GetResponseMobileSdkClient.getApi
import com.getresponse.mobile_sdk.events.GetResponseEventsService
import com.getresponse.mobile_sdk.models.GetResponseSDKSettings
import com.getresponse.mobile_sdk.models.PresetsModel
import com.getresponse.mobile_sdk.models.push_notifications.AndroidChannel
import com.getresponse.mobile_sdk.push_notifications.GetResponsePushNotificationsService
import java.util.UUID


class GetResponseMobileSDK(
    private val context: Context,
    private val applicationId: String,
    private val entryPoint: String,
    private val secretKey: String,
    private val settings: GetResponseSDKSettings = GetResponseSDKSettings(),
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

    var eventsService: GetResponseEventsService = GetResponseEventsService()
    var pushNotificationsService: GetResponsePushNotificationsService = GetResponsePushNotificationsService(context)

    suspend fun initialize() {
        val init = initializeSDK()
        init?.let { presets ->
            presets.webevents?.let {
                eventsService.configure(
                    it.options.shop,
                    it.options.user,
                    it.endpoint,
                    installationUUID,
                    settings.enableDebug,
                    true
                )
            }

            presets.mobilepush?.let {
                pushNotificationsService.configure(
                    applicationId,
                    it.endpoint,
                    secretKey,
                    settings.notificationIcon,
                    settings.channelsConfig,
                    installationUUID,
                    settings.enableDebug,
                    true
                )
            }
        }
    }

    private suspend fun initializeSDK(): PresetsModel? {
        try {
            val presets = getApi(entryPoint, createJWTToken(applicationId, secretKey, installationUUID), settings.enableDebug).presets()
            return presets
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
        }
        return null
    }

    companion object {
        private const val PREFS_KEY = "getresponse_mobile_sdk"
        private const val INSTALLATION_UUID_KEY = "installation_uuid"
        private const val TAG = "GetResponseMobileSDK"
    }
}