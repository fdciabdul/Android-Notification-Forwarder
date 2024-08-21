package com.example.notifier

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import android.content.Context

class MyNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val title = sbn.notification.extras.getString("android.title") ?: "No Title"
        val text = sbn.notification.extras.getString("android.text") ?: "No Text"

        val jsonPayload = JSONObject().apply {
            put("package", packageName)
            put("title", title)
            put("text", text)
        }

        CoroutineScope(Dispatchers.IO).launch {
            sendNotificationToServer(jsonPayload.toString())
        }

        val intent = Intent("com.example.myapplication.NOTIFICATION_POSTED").apply {
            putExtra("package", packageName)
            putExtra("title", title)
            putExtra("text", text)
        }
        sendBroadcast(intent)
    }

    private fun sendNotificationToServer(jsonPayload: String) {
        try {
            val client = OkHttpClient()

            val requestBody = RequestBody.create(
                "application/json; charset=utf-8".toMediaTypeOrNull(),
                jsonPayload
            )

            val sharedPreferences = getSharedPreferences("myAppPrefs", Context.MODE_PRIVATE)
            val serverUrl = sharedPreferences.getString("server_url", "https://notif.imtaqin.id/") // Default URL

            val request = serverUrl?.let {
                Request.Builder()
                    .url(it)
                    .post(requestBody)
                    .build()
            }

            if (request != null) {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("NotificationService", "Failed to send notification: ${response.code}")
                    } else {
                        Log.d("NotificationService", "Notification sent successfully: ${response.code}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationService", "Error sending notification: ${e.message}", e)
        }
    }

}
