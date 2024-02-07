package com.getresponse.pushtests

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.getresponse.mobile_sdk.GetResponseMobileSDK
import com.getresponse.pushtests.ui.theme.PushTestsTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


const val TAG = "MainActivity"
const val OPEN_SETTINGS_TEXT = "Should open settings"

const val applicationId = BuildConfig.applicationId
const val entryPoint = BuildConfig.entryPoint
const val secretKey = BuildConfig.secretKey
val notificationIcon = R.mipmap.ic_launcher
const val email = BuildConfig.email

const val languageCode = "en"
const val externalId = "externalId"

suspend fun updateDbToken(token: String) {
    val db = Firebase.firestore
    val auth = Firebase.auth
    val authResult = auth.signInAnonymously().await()

    if (authResult.user == null) {
        Log.e(TAG, "User is null")
        return
    }
    db.collection("tokens").document(authResult.user!!.uid).set(
        mapOf(
            "token" to token,
            "createdAt" to FieldValue.serverTimestamp()
        )
    )
}

class MainActivity : ComponentActivity() {

    private lateinit var grSdk: GetResponseMobileSDK

    private suspend fun connectMessaging(): String? {
        val token = FirebaseMessaging.getInstance().token.await()
        Log.d(TAG, "token $token")
        updateDbToken(token)
        return token
    }

    override fun onNewIntent(intent: Intent) {
        val result = grSdk.handleIncomingNotification(this, intent)
        Log.d("test newIntent", result.toString())
        super.onNewIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val grSdk = GetResponseMobileSDK(
            this,
            applicationId,
            entryPoint,
            secretKey,
            notificationIcon,
        )

        val incomingNotification = grSdk.handleIncomingNotification(this, intent)

        setContent {
            val scrollState = rememberScrollState()
            val clipboardManager: ClipboardManager = LocalClipboardManager.current
            val granted = remember { mutableStateOf(false) }
            val shouldAsk = remember { mutableStateOf("Nothing yet") }
            val token = remember { mutableStateOf<String?>(null) }
            val error = remember { mutableStateOf<String?>(null) }

            val json = remember { mutableStateOf(incomingNotification.toString()) }
            val scope = rememberCoroutineScope()

            val requestPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { _ ->
                scope.launch {
                    checkPermissions(granted, shouldAsk, token, error)
                }
            }

            val lifecycleEvent = rememberLifecycleEvent()
            LaunchedEffect(lifecycleEvent) {
                if (lifecycleEvent == Lifecycle.Event.ON_RESUME) {
                    checkPermissions(granted, shouldAsk, token, error)
                }
            }

            PushTestsTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier
                            .padding(30.dp)
                            .then(Modifier.fillMaxWidth())
                            .verticalScroll(scrollState)
                    ) {
                        Text(text = "POC Push Notifications", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(text = "Permission status:", style = MaterialTheme.typography.titleLarge)
                        Text(text = if (granted.value) "Granted" else "Not granted", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(20.dp))

                        Text(text = "Rationale status:", style = MaterialTheme.typography.titleLarge)
                        Text(text = shouldAsk.value, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(20.dp))

                        Text(text = "Intent data:", style = MaterialTheme.typography.titleLarge)
                        Text(text = json.value, style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Notificaiton data:", style = MaterialTheme.typography.titleLarge)

                        Spacer(modifier = Modifier.height(20.dp))

                        Row {
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    scope.launch {
                                        try {
                                            grSdk.consent("en", "externalId", "martom_mb@wp.pl", token.value ?: "")
                                            Toast.makeText(this@MainActivity, "Consent sent with Email", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            error.value = e.message
                                            Log.e("ERROR", e.message ?: "Unknown error", e)
                                        }
                                    }
                                }) {
                                Text("Consent with Email")
                            }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    scope.launch {
                                        try {
                                            grSdk.consent("en", "externalId", null, token.value ?: "")
                                            Toast.makeText(this@MainActivity, "Consent sent without email", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            error.value = e.message
                                            Log.e("ERROR", e.message ?: "Unknown error", e)
                                        }
                                    }
                                }) {
                                Text("Consent without Email")
                            }
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        grSdk.deleteConsent()
                                        Toast.makeText(this@MainActivity, "Deleted consent", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        error.value = e.message
                                        Log.e("ERROR", e.message ?: "Unknown error", e)
                                    }
                                }
                            }) {
                            Text("Delete consent")
                        }
                        if (token.value != null) {
                            Text(text = "FCM Token:", style = MaterialTheme.typography.titleLarge)
                            Text(text = token.value!!, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(onClick = {
                                clipboardManager.setText(AnnotatedString((token.value!!)))
                            }) {
                                Text("Copy")
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        if (error.value != null) {
                            Text(text = "Error:", style = MaterialTheme.typography.titleLarge)
                            Text(text = error.value!!, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(20.dp))
                        }


                        if (!granted.value) {
                            Button(onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) ==
                                        PackageManager.PERMISSION_GRANTED
                                    ) {
                                        scope.launch {
                                            checkPermissions(granted, shouldAsk, token, error)
                                        }
                                    } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                                        shouldAsk.value = "Should open settings"
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                        val uri = Uri.fromParts("package", packageName, null)
                                        intent.data = uri
                                        startActivity(intent)
                                    } else {
                                        // Directly ask for the permission
                                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            }) {
                                Text(
                                    text = if (shouldAsk.value == OPEN_SETTINGS_TEXT) "Open settings" else "Ask for notification permissions",
                                    style = MaterialTheme.typography.titleMedium
                                )

                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun MainActivity.checkPermissions(
        granted: MutableState<Boolean>,
        shouldAsk: MutableState<String>,
        token: MutableState<String?>,
        error: MutableState<String?>
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                granted.value = true
                shouldAsk.value = "OK"
                try {
                    token.value = connectMessaging()
                } catch (e: Exception) {
                    error.value = e.message
                }
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                shouldAsk.value = OPEN_SETTINGS_TEXT
            }
        } else {
            granted.value = true
            shouldAsk.value = "Old Android version - no need to ask"
            try {
                token.value = connectMessaging()
            } catch (e: Exception) {
                error.value = e.message
                Log.e(TAG, "Error: ${e.message}")
            }
        }
    }
}

@Composable
fun rememberLifecycleEvent(lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current): Lifecycle.Event {
    var state by remember { mutableStateOf(Lifecycle.Event.ON_ANY) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            state = event
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    return state
}