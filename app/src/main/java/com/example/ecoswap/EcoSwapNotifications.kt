package com.example.ecoswap

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object EcoSwapNotifications {

    const val CHANNEL_ID_GENERAL = "ecoswap_general"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val name = context.getString(R.string.notification_channel_name)
        val description = context.getString(R.string.notification_channel_description)
        val channel = NotificationChannel(
            CHANNEL_ID_GENERAL,
            name,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            this.description = description
        }
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33) {
            return ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    /**
     * Muestra una notificación de “hay un nuevo producto” (título del objeto).
     * Si no hay permiso en Android 13+, no hace nada.
     */
    fun notifyNewProduct(context: Context, productTitle: String, productId: Long) {
        if (!canPost(context)) return
        ensureChannel(context)

        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            productId.toInt() and 0xFFFF,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_GENERAL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.notif_new_product_title))
            .setContentText(
                context.getString(R.string.notif_new_product_body, productTitle)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        val notifyId = (productId and 0x7FFFFFFF).toInt()
        NotificationManagerCompat.from(context).notify(notifyId, notification)
    }
}
