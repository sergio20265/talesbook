package ru.norahobbits.talesbook

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import ru.norahobbits.talesbook.media.MusicPlayerService
import ru.norahobbits.talesbook.navigation.AppNavigation
import ru.norahobbits.talesbook.settings.AppSettings
import ru.norahobbits.talesbook.settings.AppSettingsDataStore
import ru.norahobbits.talesbook.ui.theme.TalesbookTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsDataStore: AppSettingsDataStore

    private var musicService: MusicPlayerService? = null
    private var musicBound = false

    private val musicConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? MusicPlayerService.LocalBinder
            musicService = binder?.getService()
            musicBound = true
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            musicBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val settings by settingsDataStore.settings.collectAsState(AppSettings())

            LaunchedEffect(settings.musicEnabled, settings.selectedMusicUri) {
                handleMusic(settings)
            }

            LaunchedEffect(settings.musicVolume) {
                if (musicBound) musicService?.setVolume(settings.musicVolume)
            }

            TalesbookTheme(appTheme = settings.selectedTheme) {
                AppNavigation(appSettings = settings)
            }
        }
    }

    private fun handleMusic(settings: AppSettings) {
        if (settings.musicEnabled && settings.selectedMusicUri != null) {
            val intent = Intent(this, MusicPlayerService::class.java).apply {
                action = MusicPlayerService.ACTION_PLAY
                putExtra(MusicPlayerService.EXTRA_URI, settings.selectedMusicUri)
                putExtra(MusicPlayerService.EXTRA_VOLUME, settings.musicVolume)
            }
            startService(intent)
            if (!musicBound) {
                bindService(intent, musicConnection, BIND_AUTO_CREATE)
            }
        } else {
            Intent(this, MusicPlayerService::class.java).also { intent ->
                intent.action = MusicPlayerService.ACTION_STOP
                startService(intent)
            }
            if (musicBound) {
                unbindService(musicConnection)
                musicBound = false
            }
        }
    }

    override fun onDestroy() {
        if (musicBound) {
            unbindService(musicConnection)
            musicBound = false
        }
        super.onDestroy()
    }
}
