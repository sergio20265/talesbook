package ru.norahobbits.talesbook.media

import android.app.*
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import ru.norahobbits.talesbook.MainActivity

class MusicPlayerService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }

    private val binder = LocalBinder()
    private var mediaPlayer: MediaPlayer? = null
    private var volume: Float = 0.5f

    companion object {
        const val ACTION_PLAY = "action_play"
        const val ACTION_STOP = "action_stop"
        const val EXTRA_URI = "extra_uri"
        const val EXTRA_VOLUME = "extra_volume"
        private const val NOTIFICATION_ID = 101
        private const val CHANNEL_ID = "talesbook_music"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val uri = intent.getStringExtra(EXTRA_URI) ?: return START_NOT_STICKY
                volume = intent.getFloatExtra(EXTRA_VOLUME, 0.5f)
                playMusic(uri)
            }
            ACTION_STOP -> {
                stopMusic()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun playMusic(uriString: String) {
        mediaPlayer?.release()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, Uri.parse(uriString))
                isLooping = true
                setVolume(volume, volume)
                prepare()
                start()
            }
            startForeground(NOTIFICATION_ID, buildNotification())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopMusic() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun setVolume(v: Float) {
        volume = v
        mediaPlayer?.setVolume(v, v)
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TalesBook")
            .setContentText("Музыка для письма")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pi)
            .setSilent(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Фоновая музыка",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        stopMusic()
        super.onDestroy()
    }
}
