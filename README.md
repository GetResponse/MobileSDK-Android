# Introduction
GetResponse Mobile SDK is a wrapper for Firebase Messaging. Because every app is different, and you as a developer may want to hook our SDK into existing codebase or even existing push handling - we provide a simple API to handle intercepting messages sent by GetResponse and API connection to register users in the backend.
Installation

Use jitpack.io to include the library in your project. Add the following to your root build.gradle file:

```groovy
    dependencyResolutionManagement {
		repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
		repositories {
			mavenCentral()
			maven { url "https://jitpack.io" }
		}
	}
```

Add library dependency
```groovy
    dependencies {
	    implementation "com.github.GetResponse:MobileSDK-Android:1.0.1"
	}
```

## Requirements
App should be written in kotlin and target Android SDK 34
App should have configured Firebase Cloud Messaging (link: https://firebase.google.com/docs/cloud-messaging/android/client)  to handle push notification
App should have configured requests for appropriate permissions

## Setup GetResponse Account

tbd...

# Setup GetResponse Mobile SDK
## Initialization

## Create instance of GetResponseMobileSDK with data created in GetResponse App

```kotlin
val grMobileSDK = GetResponseMobileSDK(
    this,
    applicationId,
    entryPoint,
    secretKey,
    notificationIcon,
    channelsConfig (optional),  
    enableDebug (optional)
)
```
Parameters:
- applicationId, entryPoint, secretKey - Data provided in GetResponse App
- notificationIcon - Notification Icon Resource ID
- channelsConfig - (optional) list of notification channels for Android SDKs that supports it. Parameter is optional, and SDK will create channels for you if they don’t exist. It will also create a new channel if channel_id provided in notification created on GetResponse website is missing.
- enableDebug - this flag enables logs for http calls.

# Messaging service - handle Incoming Push Message

This point assumes that Firebase Messaging is configured and there is a service extending FirebaseMessagingService

## Inject GetResponseMobileSDK
in the MessagingService and provide what class is your app entry point.

MessagingService:
```kotlin
override fun onMessageReceived(remoteMessage: RemoteMessage) {
    super.onMessageReceived(remoteMessage)
    val pushConsumed = grMobileSDK.handleIncomingPush(
        remoteMessage.data,
        MainActivity::class.java
    )
    Log.d(TAG, if (pushConsumed) "GetResponse notification" else "Not a GetResponse notification")
}
```
handleIncomingPush method will return boolean value. True if it detects a message sent by GetResponse, and false in all other cases.

### Update Push Notification Consent:
In order to support fcmToken changes app needs to register for this in onNewToken callback (https://firebase.google.com/docs/cloud-messaging/manage-tokens#retrieve-and-store-registration-tokens)

```kotlin
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
```

### Activity settings
Next part of setup is setting Android Activity component to register fcmToken in GetResponse platform and to handle incoming Intent after interaction with Notification.
Register for push notification in GetResponse
After obtaining fcmToken from Firebase - application has to send consent using GetResponseMobileSDK. This is typically done using method:

```kotlin
suspend fun connectMessaging() {
    val token = FirebaseMessaging.getInstance().token.await()
    grSdk.consent(languageCode, externalId, email, token)
}
```

### Consent request
In order to register a user device in the GetResponse platform app needs to send a consent request. This is done using the suspending method - consent.
Parameters:
- languageCode - desired language code for GetResponse messages
- externalId - (optional) can be used to connect users from external services in the GetResponse database.
- email - (optional) this parameter will link push notification user with GetResponse subscriber
- token - fcmToken provided by Firebase Messaging

This method should be run every time your app starts - this will ensure that only actual tokens used by users are stored in GetResponse.
Handle Notification interactions
In order to fully use the capabilities of our SDK we provide a method that should be used after receiving intent when the user interacts with notification.
In the onCreate method of the Activity previously provided in handleIncomingPush parameters add this line to read message data and count statistics for push notification.
```kotlin
    class MainActivity : ComponentActivity() {
        
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            val incomingNotification = grSdk.handleIncomingNotification(this, intent)
                
```
Return of this function is a list of parameters provided in the Administration Panel while sending Push Message.
Delete consent
This method should be run whenever you think GetResponse should stop sending messages to specific device
(primary use case is before the user logs out from the app).
```kotlin
scope.launch {
    grSdk.deleteConsent()
}
```



