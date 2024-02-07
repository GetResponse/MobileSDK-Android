package com.getresponse.pushtests

import android.util.Log
import com.getresponse.mobile_sdk.GetResponseMobileSDK
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PushService : FirebaseMessagingService() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val grMobileSDK =
        GetResponseMobileSDK(
            this,
            applicationId,
            entryPoint,
            secretKey,
            notificationIcon,
        )

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val pushConsumed = grMobileSDK.handleIncomingPush(
            remoteMessage.data,
            MainActivity::class.java
        )
        Log.d(TAG, if (pushConsumed) "GetResponse notification" else "Not a GetResponse notification")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        scope.launch {
            grMobileSDK.consent(
                languageCode,
                externalId,
                email,
                token
            )
        }
    }

    companion object {
        private const val TAG = "PushService"
    }
}
