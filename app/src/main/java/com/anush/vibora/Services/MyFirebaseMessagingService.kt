package com.anush.vibora.Services

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.anush.vibora.Activities.MainActivity
import com.anush.vibora.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "AKChatsChannel"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "onMessageReceived: ${Gson().toJson(remoteMessage)}")

        var title = "AKChats"
        var messageBody = "New message"
        var senderId = ""
        var chatId = ""
        var senderName = ""
        var type = ""

        remoteMessage.notification?.let {
            title = it.title ?: title
            messageBody = it.body ?: messageBody
        }

        if (remoteMessage.data.isNotEmpty()) {
            val data = remoteMessage.data
            senderId = data["senderId"].orEmpty()
            senderName = data["gcm.notification.title"].orEmpty()
            chatId = data["chatId"].orEmpty()
            type = data["type"].orEmpty()

            messageBody = when {
                type.equals("chatImage", ignoreCase = true) -> "You received an image"
                data.containsKey("message") -> {
                    val message = data["message"].orEmpty()
                    if (message.length > 20) message.substring(0, 20) + "..." else message
                }
                else -> messageBody
            }
        }

        showNotification(title, messageBody, senderId, senderName, chatId)

        /*if (isChatActivityOpen()) {
            updateChatUI(remoteMessage)
        } else {
            showNotification(title, messageBody, senderId, senderName, chatId)
        }*/
    }

    /*private fun isChatActivityOpen(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val taskInfo = activityManager.runningAppProcesses
        if (taskInfo.isNotEmpty()) {
            val componentInfo: ComponentName = taskInfo[0].topActivity ?: return false
            return componentInfo.className == MainActivity::class.java.name
        }
        return false
    }*/

    private fun updateChatUI(remoteMessage: RemoteMessage) {
        // You can implement live UI update if you want
    }

    private fun showNotification(title: String, message: String, senderId: String, senderName: String, chatId: String) {
        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("receiverId", senderId)
            putExtra("receiverName", senderName)
            putExtra("chatId", chatId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.vibora_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this).notify(1001, builder.build())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "AKChats Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                )
                manager.createNotificationChannel(channel)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token: $token")

        val auth = FirebaseAuth.getInstance()
        auth.currentUser?.let { user ->
            FirebaseFirestore.getInstance().collection("users")
                .document(user.uid)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d(TAG, "Token updated successfully")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to update token", e)
                }
        }
    }
}