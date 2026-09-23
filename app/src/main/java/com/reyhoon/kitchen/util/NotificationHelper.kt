package com.reyhoon.kitchen.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.reyhoon.kitchen.MainActivity
import com.reyhoon.kitchen.R

object NotificationHelper {

    private const val CHANNEL_ORDERS = "reyhoon_new_orders"
    private const val CHANNEL_STATUS = "reyhoon_status"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return

        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val orders = NotificationChannel(
            CHANNEL_ORDERS,
            "سفارش جدید ریحون",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "آلارم سفارش جدید از مشتری"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 400)
            setSound(alarmUri, attrs)
            enableLights(true)
            setBypassDnd(true)
        }

        val status = NotificationChannel(
            CHANNEL_STATUS,
            "وضعیت سفارش",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "به‌روزرسانی وضعیت سفارش"
            enableVibration(true)
        }

        nm.createNotificationChannel(orders)
        nm.createNotificationChannel(status)
    }

    fun notifyNewOrder(context: Context, count: Int, customerName: String = "") {
        ensureChannels(context)
        val title = "سفارش جدید دارید!"
        val body = if (customerName.isNotBlank()) {
            "مشتری $customerName — $count سفارش جدید منتظر تأیید است"
        } else {
            "$count سفارش جدید منتظر بررسی است"
        }

        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_orders", true)
        }
        val pi = PendingIntent.getActivity(
            context,
            1001,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ORDERS)
            .setSmallIcon(R.drawable.ic_reyhoon_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText("سفارش جدید دارید!\n$body"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 500, 250, 500, 250, 500))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                notif
            )
        } catch (_: SecurityException) {
            // permission missing on Android 13+
        }
        vibrate(context)
        playRingtone(context)
    }

    fun notifyStatus(context: Context, title: String, body: String) {
        ensureChannels(context)
        val notif = NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_reyhoon_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(
                (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                notif
            )
        } catch (_: SecurityException) {
        }
    }

    private fun playRingtone(context: Context) {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            RingtoneManager.getRingtone(context, uri)?.play()
        } catch (_: Exception) {
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrate(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1))
                } else {
                    it.vibrate(longArrayOf(0, 400, 200, 400), -1)
                }
            }
        } catch (_: Exception) {
        }
    }
}
