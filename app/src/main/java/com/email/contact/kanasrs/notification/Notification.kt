package com.email.contact.kanasrs.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.email.contact.kanasrs.database.KanaSRSDatabase
import com.email.contact.kanasrs.R
import com.email.contact.kanasrs.activity.MenuActivity
import me.leolin.shortcutbadger.ShortcutBadger


internal class NotificationHelper(context: Context) {
    private val mContext: Context = context

    @SuppressLint("UnspecifiedImmutableFlag")
    fun createNotification() {

        var numItemsToReview = 0

        for (kana in KanaSRSDatabase.getInstance(mContext).kanaDao().getKana()) {
            //Check if there is a review time, and if so, check if the current time has passed the stored review time
            // Review time is calculated during review answers and initially added when learned in lessons
            if (kana.reviewTime != null) {
                if (kana.reviewTime!! < System.currentTimeMillis()) {
                    numItemsToReview++
                }
            }
        }

        val intent = Intent(mContext, MenuActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val resultPendingIntent = PendingIntent.getActivity(
            mContext,
            0 /* Request code */, intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val mBuilder: NotificationCompat.Builder =
            NotificationCompat.Builder(mContext, NOTIFICATION_CHANNEL_ID)

        mBuilder.setSmallIcon(R.mipmap.ic_launcher)
        mBuilder.setContentTitle("Review update")
            .setContentText("You have $numItemsToReview kana to review!")
            .setAutoCancel(true)
            .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
            .setContentIntent(resultPendingIntent)
        ShortcutBadger.applyCount(mContext, numItemsToReview)
        val mNotificationManager =
            mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Kana Review",
                importance
            )
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.enableVibration(true)
            notificationChannel.vibrationPattern =
                longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            mBuilder.setChannelId(NOTIFICATION_CHANNEL_ID)
            mNotificationManager.createNotificationChannel(notificationChannel)
        }
        mNotificationManager.notify(0 /* Request Code */, mBuilder.build())
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "10001"
    }

}