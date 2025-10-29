package com.example.lab_week_08

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.HandlerThread
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlin.math.PI


class NotificationService : Service() {

    //Notification builder yang akan dipanggil
    private lateinit var notificationBuilder: NotificationCompat.Builder
    // System handler yang akan mengontrol thread, proses apa yang dieksekusi
    private lateinit var serviceHandler : Handler

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        //Buat notif di dalam startForegroundService()
        notificationBuilder = startForegroundService()

        val handlerThread = HandlerThread("SecondThread")
            .apply {start()}
        serviceHandler = android.os.Handler(handlerThread.looper)
    }

    private fun startForegroundService(): NotificationCompat.Builder {

        // Pending intent akan dieksekusi nanti dan bukan skrg
        val pendingIntent = getPendingIntent()

        // Notif menggunakan channel id untuk mengatur konfigurasi
        val channelId = createNotificationChannel()

        // Kombinasi pending intent dan channel jadi notif builder
        val notificationBuilder = getNotificationBuilder(
            pendingIntent, channelId
        )

        // Menyalakan foreground service dan notif. Akan muncul di device user
        startForeground(NOTIFICATION_ID, notificationBuilder.build())
        return notificationBuilder
    }

    private fun getPendingIntent() : PendingIntent {
        //Ngecek apakah versi API 31 keatas, jika iya maka ada flag
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) FLAG_IMMUTABLE else 0

        //  Ketika kita klik notif, maka akan di redirect ke Main Activity
        return PendingIntent.getActivity(
            this, 0, Intent(
                this,
                MainActivity::class.java
            ), flag
        )
    }

    private fun createNotificationChannel(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channelId = "001"
        val channelName = "001 Channel"

        // Memberi tahu bahwa channel priority ini memberikan suara tapi ga muncul di head notif (default)
        val channelPriority = NotificationManager.IMPORTANCE_DEFAULT

        val channel = NotificationChannel(
            channelId, channelName, channelPriority
        )

        // Ambil class notification manager
        val service = requireNotNull(
            ContextCompat.getSystemService(this, NotificationManager::class.java)
        )

        // NotificationManager akan mentrigger notification later on
        service.createNotificationChannel(channel)

        channelId
    } else { "" }

    // Build notif dengan segala konten dan konfigurasinya
    private fun getNotificationBuilder(pendingIntent: PendingIntent, channelId: String) =
        NotificationCompat.Builder(this, channelId)
            .setContentTitle("Second worker process is done")
            .setContentText("Check it out!")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setTicker("Second worker process is done, check it out!")
            .setOngoing(true)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) : Int
    {
        val returnValue = super.onStartCommand(intent, flags, startId)

        // Ambil channel id yang diberikan dari MainActivity dari Intent
        val Id = intent?.getStringExtra(EXTRA_ID)
            ?: throw IllegalStateException("Channel ID must be provided")

        // Post notif task ke handler, yang akan dieksekusi di thread yang berbeda
        serviceHandler.post {
            // Apa yang akan terjadi ketika notif di post
            countDownFromTenToZero(notificationBuilder)

            // Notifying MainActivity kalo service process / count down udh kelar
            notifyCompletion(Id)
            // Stop foreground service yang bakal close notif, tapi service tetap nyala
            stopForeground(STOP_FOREGROUND_REMOVE)
            // Stop dan destroy service
            stopSelf()

        }
        return returnValue
    }

    // Function untuk update notif untuk menampilkan count down dari 10 sampe 0
    private fun countDownFromTenToZero(notificationService: NotificationCompat.Builder) {
        // Get notification manager
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Count down dari 10 sampe 0
        for (i in 10 downTo 0) {
            Thread.sleep(1000L)

            // Update notif dengan content text
            notificationBuilder.setContentText("$i seconds until last warning")
                .setSilent(true)

            // Notify the notification manager tentang content update tadi
            notificationManager.notify(
                NOTIFICATION_ID, notificationBuilder.build()
            )
        }
    }

    private fun notifyCompletion(Id: String) {
        // Update LiveData dengan return channel id lewat Main Thread
        Handler(Looper.getMainLooper()).post {
            mutableID.value = Id
        }
    }
    companion object {
        const val NOTIFICATION_ID = 0XCA7
        const val EXTRA_ID = "Id"

        // ini LiveData yang otomatis update UI berdasarkan observasi
        private val mutableID = MutableLiveData<String>()
        val trackingCompletion: LiveData<String> = mutableID
    }
}