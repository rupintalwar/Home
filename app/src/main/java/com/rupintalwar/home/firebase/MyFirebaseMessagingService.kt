package com.rupintalwar.home.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.ktx.messaging
import com.rupintalwar.home.MainActivity
import com.rupintalwar.home.R
import java.io.ByteArrayOutputStream
import java.net.URL
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // [START receive_message]
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")

            sendNotification(remoteMessage.data)
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }
    // [END receive_message]

    // [START on_new_token]
    /**
     * Called if the FCM registration token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the
     * FCM registration token is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
        sendRegistrationToServer(token)
    }
    // [END on_new_token]

    private fun decryptImage(link: String?, iv: String?): Bitmap? {
        try {
            val url = URL(link)
            val inputStream = url.openConnection().getInputStream()

            val key = resources.getString(R.string.image_decrypt_key)

            val ivParameterSpec =  IvParameterSpec(Base64.decode(iv, Base64.DEFAULT))
            val bytes = key.toByteArray()
            val sks = SecretKeySpec(bytes, "AES")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, sks, ivParameterSpec)
            val cipherInputStream = CipherInputStream(inputStream, cipher)

            val byteArrayOutputStream = ByteArrayOutputStream()
            var nextByte: Int = cipherInputStream.read()
            while (nextByte != -1) {
                byteArrayOutputStream.write(nextByte)
                nextByte = cipherInputStream.read()
            }
            val byteArray: ByteArray = byteArrayOutputStream.toByteArray()
            Log.d(TAG, "Decrypted Image Successfully")

            return BitmapFactory.decodeByteArray(byteArray,0, byteArray.size)

        } catch (e: Exception) {
            Log.d("Exception Thrown", e.toString())
            return null
        }
    }

    private fun sendRegistrationToServer(token: String?) {
        // TODO: Implement this method to send token to your app server.
        Firebase.messaging.subscribeToTopic("sec-cam")
        Log.d(TAG, "sendRegistrationTokenToServer($token)")
    }

    private fun sendNotification(data: Map<String,String>) {
        Log.d("Sending", data.toString())
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
            PendingIntent.FLAG_IMMUTABLE)

        val image: Bitmap? = decryptImage(data["image"], data["iv"])
        val channelId = "notif_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(data["title"])
            .setContentText(data["body"])
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setLargeIcon(image)
            .setStyle(NotificationCompat.BigPictureStyle()
                .bigPicture(image)
                .bigLargeIcon(null)
            )
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId,
                "Security Cam",
                NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(notif_id, notificationBuilder.build())
        notif_id++
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
        private var notif_id = 0
    }
}