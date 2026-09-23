package com.reyhoon.kitchen.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
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

    private const val CHANNEL_ORDERS = "reyhoon_new_orders_v2"
    private const val CHANNEL_STATUS = "reyhoon_status_v2"
    const val NEW_ORDER_NOTIF_ID = 71001

    @Volatile
    private var activeRingtone: Ringtone? = null

    /** سفارش‌هایی که قبلاً آلارم شده‌اند — جلوگیری از تکرار */
    private val alertedOrderIds = mutableSetOf<String>()

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val orders = NotificationChannel(
            CHANNEL_ORDERS,
            "سفارش جدید ریحون",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "اعلان سفارش جدید از مشتری"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 350, 150, 350)
            setSound(soundUri, attrs)
            enableLights(true)
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

    /**
     * فقط برای سفارش‌های جدید (id ندیده‌شده) نوتیف می‌فرستد.
     * @return تعداد سفارش تازه‌ای که آلارم شد
     */
    fun notifyNewOrders(context: Context, orderIds: List<String>, customerName: String = ""): Int {
        val fresh = orderIds.filter { it.isNotBlank() && it !in alertedOrderIds }
        if (fresh.isEmpty()) return 0
        alertedOrderIds.addAll(fresh)

        ensureChannels(context)
        val count = fresh.size
        val title = "سفارش جدید دارید!"
        val body = if (customerName.isNotBlank()) {
            "مشتری $customerName — $count سفارش جدید"
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
            .setStyle(NotificationCompat.BigTextStyle().bigText("$title\n$body"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pi)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 350, 150, 350))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NEW_ORDER_NOTIF_ID, notif)
        } catch (_: SecurityException) {
        }
        vibrateOnce(context)
        playOnce(context)
        return count
    }

    /** سازگاری با فراخوانی‌های قبلی */
    fun notifyNewOrder(context: Context, count: Int, customerName: String = "") {
        // بدون id مشخص — یک بار با کلید ساختگی
        notifyNewOrders(
            context,
            listOf("batch-${System.currentTimeMillis()}"),
            customerName
        )
    }

    fun stopAlarm(context: Context) {
        try {
            activeRingtone?.stop()
        } catch (_: Exception) {
        }
        activeRingtone = null
        try {
            NotificationManagerCompat.from(context).cancel(NEW_ORDER_NOTIF_ID)
        } catch (_: Exception) {
        }
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.cancel()
        } catch (_: Exception) {
        }
    }

    fun notifyStatus(context: Context, title: String, body: String) {
        ensureChannels(context)
        val notif = NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_reyhoon_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(
                (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                notif
            )
        } catch (_: SecurityException) {
        }
    }

    private fun playOnce(context: Context) {
        try {
            activeRingtone?.stop()
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ring = RingtoneManager.getRingtone(context, uri)
            activeRingtone = ring
            ring?.play()
        } catch (_: Exception) {
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrateOnce(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // فقط یک الگو — بدون تکرار نامحدود (-1)
                    it.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 350, 150, 350), -1).let { effect ->
                        // API نمی‌گذارد waveform بدون repeat؛ یک‌بار با createOneShot کافی است
                        VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
                    })
                } else {
                    it.vibrate(500)
                }
            }
        } catch (_: Exception) {
        }
    }
}
