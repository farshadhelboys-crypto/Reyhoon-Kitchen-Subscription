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

    private const val CHANNEL_ORDERS = "reyhoon_new_orders_v3"
    private const val CHANNEL_STATUS = "reyhoon_status_v3"
    const val NEW_ORDER_NOTIF_ID = 71001

    @Volatile
    private var activeRingtone: Ringtone? = null

    /** تا وقتی کاربر صفحه سفارش آنلاین را باز نکند true می‌ماند */
    @Volatile
    var pendingAlarm: Boolean = false
        private set

    private val knownOrderIds = mutableSetOf<String>()

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ORDERS, "سفارش جدید ریحون", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "تا باز کردن سفارش آنلاین ادامه دارد"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
                setSound(soundUri, attrs)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_STATUS, "وضعیت سفارش", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    /**
     * سفارش‌های تازه را ثبت می‌کند و آلارم را روشن نگه می‌دارد
     * تا وقتی [acknowledgeOrdersViewed] صدا شود.
     */
    fun onNewOrdersDetected(context: Context, orderIds: List<String>, customerName: String = ""): Int {
        val fresh = orderIds.filter { it.isNotBlank() && it !in knownOrderIds }
        if (fresh.isEmpty() && !pendingAlarm) return 0
        knownOrderIds.addAll(fresh)
        if (fresh.isNotEmpty()) pendingAlarm = true

        if (!pendingAlarm) return 0

        ensureChannels(context)
        val title = "سفارش آنلاین جدید!"
        val body = if (customerName.isNotBlank()) {
            "$customerName — برای قطع آلارم سفارش آنلاین را باز کنید"
        } else {
            "برای قطع آلارم، سفارش‌های آنلاین را باز کنید"
        }

        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_orders", true)
        }
        val pi = PendingIntent.getActivity(
            context, 1001, open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ORDERS)
            .setSmallIcon(R.drawable.ic_reyhoon_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$title\n$body"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true) // تا باز کردن صفحه قطع نشود
            .setAutoCancel(false)
            .setContentIntent(pi)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 400, 200, 400))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NEW_ORDER_NOTIF_ID, notif)
        } catch (_: SecurityException) {
        }
        vibrateOnce(context)
        playOnce(context)
        return fresh.size
    }

    /** فقط وقتی صفحه سفارش آنلاین باز شد */
    fun acknowledgeOrdersViewed(context: Context) {
        pendingAlarm = false
        stopSoundAndNotif(context)
    }

    fun stopSoundAndNotif(context: Context) {
        try { activeRingtone?.stop() } catch (_: Exception) {}
        activeRingtone = null
        try {
            NotificationManagerCompat.from(context).cancel(NEW_ORDER_NOTIF_ID)
        } catch (_: Exception) {}
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.cancel()
        } catch (_: Exception) {}
    }

    /** سازگاری قدیمی */
    fun notifyNewOrders(context: Context, orderIds: List<String>, customerName: String = "") =
        onNewOrdersDetected(context, orderIds, customerName)

    fun notifyNewOrder(context: Context, count: Int, customerName: String = "") {
        onNewOrdersDetected(context, listOf("batch-${System.currentTimeMillis()}"), customerName)
    }

    fun stopAlarm(context: Context) {
        // عمداً فقط صدا را قطع نمی‌کند مگر acknowledge
        // برای سازگاری با کد قدیمی: اگر pending نباشد قطع کن
        if (!pendingAlarm) stopSoundAndNotif(context)
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
                (System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notif
            )
        } catch (_: SecurityException) {}
    }

    private fun playOnce(context: Context) {
        try {
            activeRingtone?.stop()
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ring = RingtoneManager.getRingtone(context, uri)
            activeRingtone = ring
            ring?.play()
        } catch (_: Exception) {}
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
                    it.vibrate(VibrationEffect.createOneShot(450, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    it.vibrate(450)
                }
            }
        } catch (_: Exception) {}
    }
}
