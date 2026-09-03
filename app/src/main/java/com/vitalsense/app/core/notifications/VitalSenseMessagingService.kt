package com.vitalsense.app.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.vitalsense.app.MainActivity
import com.vitalsense.app.R

/**
 * Firebase Cloud Messaging Service for VitalSense.
 * Receives FCM push notifications for Live Queue calls, turn proximity alerts, and emergency SOS.
 */
class VitalSenseMessagingService : FirebaseMessagingService() {

    private val TAG = "VitalSenseFCM"

    companion object {
        const val QUEUE_CHANNEL_ID = "vitalsense_queue_channel"
        const val SOS_CHANNEL_ID = "vitalsense_sos_channel"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Registration Token: $token")
        // Token can be sent to backend or stored in User profile if required
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val notification = remoteMessage.notification

        val title = notification?.title ?: data["title"] ?: "VitalSense Queue Alert"
        val message = notification?.body ?: data["message"] ?: data["body"] ?: "You have a new update."
        val type = data["type"] ?: "QUEUE_UPDATE"

        showNotification(title, message, type)
    }

    private fun showNotification(title: String, message: String, type: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = if (type.contains("SOS")) SOS_CHANNEL_ID else QUEUE_CHANNEL_ID

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = if (type.contains("SOS")) "Emergency SOS Alerts" else "Live Queue & Appointments"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "VitalSense live health notifications"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationId = (System.currentTimeMillis() % 100000).toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
