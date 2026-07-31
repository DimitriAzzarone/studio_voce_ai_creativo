package com.dimitriazzarone.studiovoceai.audio

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

enum class AudioPlaybackStatus {
    NO_FILE,
    LOADING,
    READY,
    PLAYING,
    PAUSED,
    ENDED,
    ERROR
}

class AudioPlayerController(context: Context) {
    private val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build()

    var status by mutableStateOf(AudioPlaybackStatus.NO_FILE)
        private set
    var positionMs by mutableLongStateOf(0L)
        private set
    var durationMs by mutableLongStateOf(0L)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    private val positionHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val positionUpdater = object : Runnable {
        override fun run() {
            updateTimes()
            positionHandler.postDelayed(this, 250L)
        }
    }

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                updateTimes()
                status = when (playbackState) {
                    Player.STATE_BUFFERING -> AudioPlaybackStatus.LOADING
                    Player.STATE_READY -> {
                        if (player.isPlaying) AudioPlaybackStatus.PLAYING
                        else if (positionMs > 0L) AudioPlaybackStatus.PAUSED
                        else AudioPlaybackStatus.READY
                    }
                    Player.STATE_ENDED -> AudioPlaybackStatus.ENDED
                    Player.STATE_IDLE -> {
                        if (status == AudioPlaybackStatus.NO_FILE) {
                            AudioPlaybackStatus.NO_FILE
                        } else {
                            status
                        }
                    }
                    else -> status
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateTimes()
                if (player.playbackState == Player.STATE_READY) {
                    status = if (isPlaying) {
                        AudioPlaybackStatus.PLAYING
                    } else if (positionMs > 0L) {
                        AudioPlaybackStatus.PAUSED
                    } else {
                        AudioPlaybackStatus.READY
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                status = AudioPlaybackStatus.ERROR
                errorMessage = "Impossibile riprodurre questo file audio: ${error.errorCodeName}."
                updateTimes()
            }
        })
        positionHandler.post(positionUpdater)
    }

    fun prepare(uri: Uri) {
        errorMessage = null
        positionMs = 0L
        durationMs = 0L
        status = AudioPlaybackStatus.LOADING
        try {
            player.stop()
            player.clearMediaItems()
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
        } catch (_: SecurityException) {
            status = AudioPlaybackStatus.ERROR
            errorMessage = "L'app non può più accedere al file. Selezionalo nuovamente."
        } catch (_: IllegalArgumentException) {
            status = AudioPlaybackStatus.ERROR
            errorMessage = "Il file selezionato non contiene un indirizzo audio valido."
        } catch (_: RuntimeException) {
            status = AudioPlaybackStatus.ERROR
            errorMessage = "Errore durante la preparazione del file audio."
        }
    }

    fun play() {
        if (status == AudioPlaybackStatus.NO_FILE || status == AudioPlaybackStatus.ERROR) return
        if (status == AudioPlaybackStatus.ENDED) {
            player.seekTo(0L)
        }
        player.play()
    }

    fun pause() {
        if (player.isPlaying) player.pause()
    }

    fun stop() {
        if (status == AudioPlaybackStatus.NO_FILE) return
        player.pause()
        player.seekTo(0L)
        positionMs = 0L
        status = if (player.playbackState == Player.STATE_READY) {
            AudioPlaybackStatus.READY
        } else {
            AudioPlaybackStatus.LOADING
        }
    }

    fun seekTo(position: Long) {
        if (status == AudioPlaybackStatus.NO_FILE || status == AudioPlaybackStatus.ERROR) return
        val safeDuration = durationMs.takeIf { it > 0L }
        val target = if (safeDuration != null) position.coerceIn(0L, safeDuration) else position.coerceAtLeast(0L)
        player.seekTo(target)
        positionMs = target
    }

    fun clear() {
        player.stop()
        player.clearMediaItems()
        positionMs = 0L
        durationMs = 0L
        errorMessage = null
        status = AudioPlaybackStatus.NO_FILE
    }

    private fun updateTimes() {
        positionMs = player.currentPosition.coerceAtLeast(0L)
        durationMs = player.duration.takeIf { it > 0L && it != androidx.media3.common.C.TIME_UNSET } ?: 0L
    }

    fun release() {
        positionHandler.removeCallbacks(positionUpdater)
        player.release()
    }
}
